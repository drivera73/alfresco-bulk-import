/*
 * Copyright (C) 2007-2016 Peter Monks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is part of an unsupported extension to Alfresco.
 *
 */

package org.alfresco.extension.bulkimport.impl;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.alfresco.service.cmr.version.Version;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.armedia.commons.utilities.Tools;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintException;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

import org.alfresco.extension.bulkimport.BulkImportStatus;
import org.alfresco.extension.bulkimport.DryRun;
import org.alfresco.extension.bulkimport.DryRunException;
import org.alfresco.extension.bulkimport.source.BulkImportItem;
import org.alfresco.extension.bulkimport.source.BulkImportItemVersion;
import org.alfresco.extension.bulkimport.source.BulkImportTools;

import static org.alfresco.extension.bulkimport.util.Utils.*;
import static org.alfresco.extension.bulkimport.util.LogUtils.*;


/**
 * This class implements the logic for importing a batch into Alfresco.
 *
 * @author Peter Monks (pmonks@gmail.com)
 *
 */
public final class BatchImporterImpl
    implements BatchImporter
{
    private final static Log log = LogFactory.getLog(BatchImporterImpl.class);

    private final static String REGEX_SPLIT_PATH_ELEMENTS = "[\\\\/]+";

    private static final StoreRef DRY_RUN_STORE = new StoreRef("dryrun", "FakeStore");
    private static final NodeRef DRY_RUN_CREATED_NODEREF = new NodeRef(DRY_RUN_STORE, "dry-run-fake-created-node-ref");

    private final ServiceRegistry serviceRegistry;
    private final BehaviourFilter behaviourFilter;
    private final NodeService     nodeService;
    private final VersionService  versionService;
    private final ContentService  contentService;


    private final WritableBulkImportStatus importStatus;

    private final ConcurrentMap<String, NodeRef> parentCache;


    public BatchImporterImpl(final ServiceRegistry          serviceRegistry,
                             final BehaviourFilter          behaviourFilter,
                             final WritableBulkImportStatus importStatus)
    {
        // PRECONDITIONS
        assert serviceRegistry != null : "serviceRegistry must not be null.";
        assert behaviourFilter != null : "behaviourFilter must not be null.";
        assert importStatus    != null : "importStatus must not be null.";

        // Body
        this.serviceRegistry = serviceRegistry;
        this.behaviourFilter = behaviourFilter;
        this.importStatus    = importStatus;

        this.nodeService    = serviceRegistry.getNodeService();
        this.versionService = serviceRegistry.getVersionService();
        this.contentService = serviceRegistry.getContentService();
        this.parentCache    = new ConcurrentHashMap<>();
    }

    public final void resetCaches()
    {
        this.parentCache.clear();
    }

    /**
     * @see org.alfresco.extension.bulkimport.impl.BatchImporter#importBatch(String, NodeRef, Batch, boolean, boolean, boolean)
     */
    @Override
    public final void importBatch(final String  userId,
                                  final NodeRef  target,
                                  final Batch<?> batch,
                                  final boolean  replaceExisting,
                                  final boolean  pessimistic,
                                  final boolean  dryRun)
        throws InterruptedException,
               OutOfOrderBatchException
    {
        long start = System.nanoTime();

        final String batchName = "Batch #" + batch.getNumber() + ", " + batch.size() + " items, " + batch.sizeInBytes() + " bytes.";
        if (debug(log)) debug(log, "Importing " + batchName);
        importStatus.setCurrentlyImporting(batchName);

        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            @Override
            public Object doWork()
                throws Exception
            {
                if (dryRun)
                {
                    importBatchImpl(target, batch, replaceExisting, false, dryRun);
                }
                else
                {
                    importBatchInTxn(target, batch, replaceExisting, pessimistic, dryRun);
                }
                return(null);
            }
        }, userId);

        if (debug(log))
        {
            long end = System.nanoTime();
            debug(log, "Batch #" + batch.getNumber() + " (containing " + batch.size() + " nodes) processed in " + getDurationInSeconds(end - start) + ".");
        }
    }


    private final <T extends BulkImportItemVersion>
    void importBatchInTxn(final NodeRef  target,
                          final Batch<T> batch,
                          final boolean  replaceExisting,
                          final boolean  pessimistic,
                          final boolean  dryRun)
        throws InterruptedException,
               OutOfOrderBatchException
    {
        RetryingTransactionHelper txnHelper = serviceRegistry.getRetryingTransactionHelper();

        txnHelper.doInTransaction(new RetryingTransactionCallback<Object>()
        {
            @Override
            public Object execute()
                throws Exception
            {
                // Disable the auditable aspect's behaviours for this transaction, to allow creation & modification dates to be set
                behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);

                importBatchImpl(target, batch, replaceExisting, pessimistic, dryRun);
                return(null);
            }
        },
        false,   // read only flag, false=R/W txn
        false);  // requires new txn flag, false=does not require a new txn if one is already in progress (which should never be the case here)

        importStatus.batchCompleted(batch);
    }


    private final <T extends BulkImportItemVersion>
    void importBatchImpl(final NodeRef  target,
                         final Batch<T> batch,
                         final boolean  replaceExisting,
                         final boolean  pessimistic,
                         final boolean  useDryRun)
        throws InterruptedException
    {
        if (batch != null)
        {
            for (final BulkImportItem<T> item : batch)
            {
                if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");

                final DryRun<T> dryRun = (useDryRun ? new DryRun<>(item) : null);
                try
                {
                    importItem(target, item, replaceExisting, dryRun);
                    // If the dry run has non-final faults, we need to "upgrade" them...
                    if (dryRun != null && dryRun.hasFaults()) throw new DryRunException(dryRun);
                }
                catch (Throwable t)
                {
                    if (!useDryRun && pessimistic)
                    {
                    	// If this isn't a dry run, and we're being pessimistic, we fail immediately
                    	// but provide information about the item that failed
                        throw new ItemImportException(item, t);
                    }
                    else
                    {
                    	// If this is a dry run, or we're being optimistic, we report the error,
                    	// but we don't fail the batch
                    	importStatus.unexpectedError(BulkImportTools.getCompleteTargetPath(item), t);
                    }
                }
            }
        }
    }

    private final <T extends BulkImportItemVersion>
    void importItem(final NodeRef           target,
                    final BulkImportItem<T> item,
                    final boolean           replaceExisting,
                    final DryRun<T>         dryRun)
        throws InterruptedException
    {
        try
        {
            if (trace(log)) trace(log, "Importing " + (item.isDirectory() ? "directory " : "file ") + String.valueOf(item) + ".");

            NodeRef nodeRef     = findOrCreateNode(target, item, replaceExisting, dryRun);
            boolean isDirectory = item.isDirectory();

            if (nodeRef != null)
            {
                // We're creating or replacing the item, so import it
                if (isDirectory)
                {
                    importDirectory(nodeRef, item, dryRun);
                }
                else
                {
                    importFile(nodeRef, item, dryRun);
                }
            }
            // Make sure we fail this item appropriately
            if (trace(log)) trace(log, "Finished importing " + String.valueOf(item));
        }
        catch (final InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw ie;
        }
        catch (final DryRunException e)
        {
            // Do nothing - this will be handled in the caller
            throw e;
        }
        catch (final OutOfOrderBatchException oobe)
        {
            throw oobe;
        }
        catch (final Exception e)
        {
            // Capture the item that failed, along with the exception
            throw new ItemImportException(item, e);
        }
    }

    private NodeRef getParent(final NodeRef target, final BulkImportItem<?> item)
    {
        final String itemParentPath = BulkImportTools.getRelativeTargetPath(item);
        return ConcurrentUtils.createIfAbsentUnchecked(this.parentCache, itemParentPath, new ConcurrentInitializer<NodeRef>()
        {
            @Override
            public NodeRef get() throws ConcurrentException
            {
                List<String> itemParentPathElements = (itemParentPath == null || itemParentPath.length() == 0) ? null : Arrays.asList(itemParentPath.split(REGEX_SPLIT_PATH_ELEMENTS));

                // If the item is meant to be a direct child of the target folder, return it immediately
                if (itemParentPathElements == null || itemParentPathElements.isEmpty()) return target;

                if (debug(log)) debug(log, "Finding parent folder '" + itemParentPath + "'.");

                FileInfo fileInfo = null;
                try
                {
                    //####TODO: I THINK THIS WILL FAIL IN THE PRESENCE OF CUSTOM NAMESPACES / PARENT ASSOC QNAMES!!!!
                    fileInfo = serviceRegistry.getFileFolderService().resolveNamePath(target, itemParentPathElements, false);
                }
                catch (final FileNotFoundException fnfe)  // This should never be triggered due to the last parameter in the resolveNamePath call, but just in case
                {
                    throw new OutOfOrderBatchException(itemParentPath, fnfe);
                }

                // Out of order batch submission (child arrived before parent)
                if (fileInfo == null)
                {
                    throw new OutOfOrderBatchException(itemParentPath);
                }

                return fileInfo.getNodeRef();
            }
        });
    }

    private NodeRef cacheNode(final BulkImportItem<?> item, NodeRef nodeRef)
    {
        if (nodeRef == null) return null;
        String itemPath = BulkImportTools.getRelativeTargetPath(item);
        if (itemPath == null || itemPath.length() == 0)
        {
            itemPath = item.getTargetName();
        }
        else
        {
            itemPath = String.format("%s/%s", itemPath, item.getTargetName());
        }
        return ConcurrentUtils.putIfAbsent(this.parentCache, itemPath, nodeRef);
    }

    private final <T extends BulkImportItemVersion>
    NodeRef findOrCreateNode(final NodeRef           target,
                                           final BulkImportItem<T> item,
                                           final boolean           replaceExisting,
                                           final DryRun<T>         dryRun)
    {
        NodeRef result           = null;
        String  nodeName         = item.getTargetName();
        String  nodeNamespace    = item.getNamespace();
        QName   nodeQName        = QName.createQName(nodeNamespace == null ? NamespaceService.CONTENT_MODEL_1_0_URI : nodeNamespace,
                                                     QName.createValidLocalName(nodeName));
        boolean isDirectory      = item.isDirectory();
        String  parentAssoc      = item.getParentAssoc();
        QName   parentAssocQName = parentAssoc == null ? ContentModel.ASSOC_CONTAINS : createQName(serviceRegistry, parentAssoc);

        NodeRef parentNodeRef = null;
        try
        {
            parentNodeRef = getParent(target, item);
        }
        catch (final OutOfOrderBatchException oobe)
        {
            if (dryRun != null)
            {
                dryRun.addItemFault(String.format("Missing parent path [%s]", BulkImportTools.getRelativeTargetPath(item)));
                parentNodeRef = DRY_RUN_CREATED_NODEREF;
            }
            else
            {
                throw oobe;
            }
        }

        // This should never happen, but cover for it anyway...
        if (parentNodeRef == null)
        {
            parentNodeRef = target;
        }

        // Find the node
        if (trace(log)) trace(log, "Searching for node with name '" + nodeName + "' within node '" + String.valueOf(parentNodeRef) + "' with parent association '" + String.valueOf(parentAssocQName) + "'.");
        
        if ((dryRun == null) || !DRY_RUN_STORE.equals(parentNodeRef.getStoreRef()))
        {
            result = nodeService.getChildByName(parentNodeRef, parentAssocQName, nodeName);
        }

        if (result == null)    // We didn't find it, so create a new node in the repo.
        {
            String itemType      = item.getVersions().first().getType();
            QName  itemTypeQName = itemType == null ? (isDirectory ? ContentModel.TYPE_FOLDER : ContentModel.TYPE_CONTENT) : createQName(serviceRegistry, itemType);

            if (trace(log)) trace(log, "Creating new node of type '" + String.valueOf(itemTypeQName) + "' with qname '" + String.valueOf(nodeQName) + "' within node '" + String.valueOf(parentNodeRef) + "' with parent association '" + String.valueOf(parentAssocQName) + "'.");
            Map<QName, Serializable> props = new HashMap<>();
            props.put(ContentModel.PROP_NAME, nodeName);
            if (dryRun != null)
            {
                result = DRY_RUN_CREATED_NODEREF;
            }
            else
            {
                result = nodeService.createNode(parentNodeRef, parentAssocQName, nodeQName, itemTypeQName, props).getChildRef();
            }
        }
        else if (replaceExisting)
        {
            if (trace(log)) trace(log, "Found content node '" + String.valueOf(result) + "', replacing it.");
        }
        else
        {
            if (info(log)) info(log, "Skipping '" + item.getTargetName() + "' as it already exists in the repository and 'replace existing' is false.");
            result = null;
            importStatus.incrementTargetCounter(BulkImportStatus.TARGET_COUNTER_NODES_SKIPPED);
        }

        return (item.isDirectory() ? cacheNode(item, result) : result);
    }

    private final <T extends BulkImportItemVersion>
    void importDirectory(final NodeRef           nodeRef,
                         final BulkImportItem<T> item,
                         final DryRun<T>         dryRun)
        throws InterruptedException
    {
        if (item.getVersions() != null &&
            item.getVersions().size() > 0)
        {
            if (item.getVersions().size() > 1)
            {
                warn(log, "Skipping versions for directory '" + item.getTargetName() + "' - Alfresco does not support versioned spaces.");
            }

            final T lastVersion = item.getVersions().last();

            if (lastVersion.hasContent())
            {
                warn(log, "Skipping content for directory '" + item.getTargetName() + "' - Alfresco doesn't support content in spaces.");
            }

            // Import the last version's metadata only
            importVersionMetadata(nodeRef, item, lastVersion, dryRun);
        }
        else
        {
            if (trace(log)) trace(log, "No metadata to import for directory '" + item.getTargetName() + "'.");
        }

        if (trace(log)) trace(log, "Finished importing metadata for directory " + item.getTargetName() + ".");
    }


    private final <T extends BulkImportItemVersion>
    void importFile(final NodeRef           nodeRef,
                    final BulkImportItem<T> item,
                    final DryRun<T>         dryRun)
        throws InterruptedException
    {
        final int numberOfVersions = item.getVersions().size();

        if (numberOfVersions == 0)
        {
            throw new IllegalStateException(item.getTargetName() + " (being imported into " + String.valueOf(nodeRef) + ") has no versions.");
        }
        else if (numberOfVersions == 1)
        {
            importVersion(nodeRef, item, null, item.getVersions().first(), dryRun, true);
        }
        else
        {
            final T firstVersion = item.getVersions().first();
            T previousVersion = null;

            // Add the cm:versionable aspect if it isn't already there
            if (firstVersion.getAspects() == null ||
                firstVersion.getAspects().isEmpty() ||
                (!firstVersion.getAspects().contains(ContentModel.ASPECT_VERSIONABLE.toString()) &&
                 !firstVersion.getAspects().contains(ContentModel.ASPECT_VERSIONABLE.toPrefixString())))
            {
                if (debug(log)) debug(log, item.getTargetName() + " has versions but is missing the cm:versionable aspect. Adding it.");
                if (dryRun == null) nodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, null);
            }

            for (final T version : item.getVersions())
            {
                if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");

                importVersion(nodeRef, item, previousVersion, version, dryRun, false);
                previousVersion = version;
            }
        }

        if (trace(log)) trace(log, "Finished importing " + numberOfVersions + " version" + (numberOfVersions == 1 ? "" : "s") + " of file " + item.getTargetName() + ".");
    }


    private final <T extends BulkImportItemVersion>
    void importVersion(final NodeRef           nodeRef,
                       final BulkImportItem<T> item,
                       final T                 previousVersion,
                       final T                 version,
                       final DryRun<T>         dryRun,
                       final boolean           onlyOneVersion)
        throws InterruptedException
    {
        Map<String, Serializable> versionProperties = new HashMap<>();
        boolean                   isMajor           = true;

        if (version == null)
        {
            throw new IllegalStateException("version was null. This is indicative of a bug in the chosen import source.");
        }

        importVersionContentAndMetadata(nodeRef, item, version, dryRun);

        if (previousVersion != null && version.getVersionNumber() != null)
        {
            final BigDecimal difference = version.getVersionNumber().subtract(previousVersion.getVersionNumber());

            isMajor = difference.compareTo(BigDecimal.ONE) >= 0;
        }

        // Note: PROP_VERSION_LABEL is a "reserved" property, and cannot be modified by custom code.
        // In other words, we can't use the source's version label as the version label in Alfresco.  :-(
        // See: https://github.com/pmonks/alfresco-bulk-import/issues/13
//        versionProperties.put(ContentModel.PROP_VERSION_LABEL.toString(), String.valueOf(version.getVersionNumber().toString()));

        versionProperties.put(VersionModel.PROP_VERSION_TYPE, isMajor ? VersionType.MAJOR : VersionType.MINOR);

        if (version.getVersionComment() != null)
        {
            versionProperties.put(Version.PROP_DESCRIPTION, version.getVersionComment());
        }

        // Only create versions if we have to - this is an exceptionally expensive operation in Alfresco
        if (onlyOneVersion)
        {
            if (trace(log)) trace(log, "Skipping creation of a version for node '" + String.valueOf(nodeRef) + "' as it only has one version.");
        }
        else
        {
            if (trace(log)) trace(log, "Creating " + (isMajor ? "major" : "minor") + " version of node '" + String.valueOf(nodeRef) + "'.");
            if (dryRun == null) versionService.createVersion(nodeRef, versionProperties);
        }
    }


    
    private final <T extends BulkImportItemVersion> 
    void importVersionContentAndMetadata(final NodeRef           nodeRef,
                                         final BulkImportItem<T> item,
                                         final T                 version,
                                         final DryRun<T>         dryRun)
        throws InterruptedException
    {
        if (version.hasMetadata())
        {
            importVersionMetadata(nodeRef, item, version, dryRun);
        }

        if (version.hasContent())
        {
            importVersionContent(nodeRef, item, version, dryRun);
        }
    }


    private final <T extends BulkImportItemVersion>
    void importVersionMetadata(final NodeRef           nodeRef,
                               final BulkImportItem<T> item,
                               final T                 version,
                               final DryRun<T>         dryRun)
        throws InterruptedException
    {
        String                    type     = version.getType();
        Set<String>               aspects  = version.getAspects();
        Map<String, Serializable> metadata = version.getMetadata();
        DictionaryService         dictionary = serviceRegistry.getDictionaryService();

        TypeDefinition typeDef = null;
        Map<QName, AspectDefinition> definedAspects = null;
        if (type != null)
        {
            if (trace(log)) trace(log, "Setting type of '" + String.valueOf(nodeRef) + "' to '" + String.valueOf(type) + "'.");
            QName typeQname = createQName(serviceRegistry, type);
            if (dryRun != null)
            {
                typeDef = dictionary.getType(typeQname);
                if (typeDef == null)
                {
                    dryRun.addVersionFault(version, String.format("Missing Type [%s]", type));
                    throw new DryRunException(dryRun);
                }
                // Add the base type's aspects
                for (AspectDefinition def : typeDef.getDefaultAspects(true))
                {
                    if (definedAspects == null)
                    {
                        definedAspects = new LinkedHashMap<>();
                    }
                    definedAspects.put(def.getName(), def);
                }
            }
            else
            {
                nodeService.setType(nodeRef, typeQname);
            }
        }

        if (aspects != null)
        {
            boolean missingAspects = false;
            for (final String aspect : aspects)
            {
                if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");

                if (trace(log)) trace(log, "Adding aspect '" + aspect + "' to '" + String.valueOf(nodeRef) + "'.");
                QName aspectQname = createQName(serviceRegistry, aspect);
                if (dryRun != null)
                {
                    if (definedAspects == null)
                    {
                        definedAspects = new LinkedHashMap<>();
                    }
                    AspectDefinition def = dictionary.getAspect(aspectQname);
                    if (def == null)
                    {
                        dryRun.addVersionFault(version, String.format("Missing Aspect [%s]", type));
                        missingAspects = true;
                    }
                    if (!definedAspects.containsKey(def.getName())) definedAspects.put(def.getName(), def);
                }
                else
                {
                    nodeService.addAspect(nodeRef, aspectQname, null);
                }
            }
            if (missingAspects) throw new DryRunException(dryRun);
        }

        if (version.hasMetadata())
        {
            if (metadata == null) throw new IllegalStateException("The import source has logic errors - it says it has metadata, but the metadata is null.");


            // QName all the keys.  It's baffling that NodeService doesn't have a method that accepts a Map<String, Serializable>, when things like VersionService do...
            Map<QName, Serializable> qNamedMetadata = new HashMap<>(metadata.size());

            for (final String key : metadata.keySet())
            {
                if (importStatus.isStopping() || Thread.currentThread().isInterrupted()) throw new InterruptedException(Thread.currentThread().getName() + " was interrupted. Terminating early.");

                QName        keyQName = createQName(serviceRegistry, key);
                Serializable value    = metadata.get(key);

                qNamedMetadata.put(keyQName, value);
            }

            if (dryRun != null)
            {
                // Make sure to patch this up - some objects don't need the name property to be defined so we
                // leave them be as such...
                if (!qNamedMetadata.containsKey(ContentModel.PROP_NAME))
                {
                    qNamedMetadata.put(ContentModel.PROP_NAME, item.getTargetName());
                }

                // Step 1: make a list of all the attributes in the aspects and object type
                Map<QName, PropertyDefinition> propDef = new HashMap<>();
                propDef.putAll(typeDef.getProperties());
                if (definedAspects != null)
                {
                    for (AspectDefinition aspect : definedAspects.values())
                    {
                        propDef.putAll(aspect.getProperties());
                    }
                }

                // Step 2: make sure all properties that are required are present, and that all property values
                // match any enabled constraints
                for (PropertyDefinition property : propDef.values())
                {
                    final QName propertyName = property.getName();
                    final String propertyNS = propertyName.getNamespaceURI();
                    
                    if (NamespaceService.SYSTEM_MODEL_1_0_URI.equals(propertyNS))
                    {
                        // We won't check for system properties, as this isn't a common thing
                        // and if they're missing, the system will (usually) fill them in 
                        continue;
                    }

                    final Serializable value = qNamedMetadata.get(propertyName);
                    if (property.isMandatory() && property.isMandatoryEnforced() && (value == null))
                    {
                        dryRun.addVersionFault(version, String.format("Missing mandatory property [%s]", property.getName()));
                        continue;
                    }
                    
                    // TODO: double-check if the value is compatible with the target type

                    for (ConstraintDefinition constraint : property.getConstraints())
                    {
                        try
                        {
                            constraint.getConstraint().evaluate(value);
                        }
                        catch (ConstraintException e)
                        {
                            dryRun.addVersionFault(version, String.format("Constraint [%s] violation on property [%s]", constraint.getName(), property.getName()));
                        }
                    }
                }
            }

            try
            {
                if (trace(log)) trace(log, "Adding the following properties to '" + String.valueOf(nodeRef) +
                                           "':\n" + Arrays.toString(qNamedMetadata.entrySet().toArray()));
                if (dryRun == null) nodeService.addProperties(nodeRef, qNamedMetadata);
            }
            catch (final InvalidNodeRefException inre)
            {
                if (!nodeRef.equals(inre.getNodeRef()))
                {
                    // Caused by an invalid NodeRef in the metadata (e.g. in an association)
                    throw new IllegalStateException("Invalid nodeRef found in metadata file '" + version.getMetadataSource() + "'.  " +
                                                    "Probable cause: an association is being populated via metadata, but the " +
                                                    "NodeRef for the target of that association ('" + inre.getNodeRef() + "') is invalid.  " +
                                                    "Please double check your metadata file and try again.", inre);
                }
                else
                {
                    // Logic bug in the BFSIT.  :-(
                    throw inre;
                }
            }
        }
    }


    private final <T extends BulkImportItemVersion>
    void importVersionContent(final NodeRef           nodeRef,
                              final BulkImportItem<T> item,
                              final T                 version,
                              final DryRun<T>         dryRun)
        throws InterruptedException
    {
        if (version.hasContent())
        {
            if (version.contentIsInPlace())
            {
                if (trace(log)) trace(log, "Content for node '" + String.valueOf(nodeRef) + "' is in-place.");

                Map<String, Serializable> metadata = version.getMetadata();
                if (!version.hasMetadata() || metadata == null ||
                    (!metadata.containsKey(ContentModel.PROP_CONTENT.toPrefixString()) &&
                     !metadata.containsKey(ContentModel.PROP_CONTENT.toString())))
                {
                    if (dryRun != null)
                    {
                        dryRun.addVersionFault(version, "Object with in-place content missing the content property");
                        throw new DryRunException(dryRun);
                    }

                    throw new IllegalStateException("The source system you selected is incorrectly implemented - it is reporting" +
                                                    " that content is in place for '" + version.getContentSource() +
                                                    "', but the metadata doesn't contain the '" + String.valueOf(ContentModel.PROP_CONTENT) +
                                                    "' property.");
                }

                if (dryRun != null)
                {
                    Serializable content = metadata.get(ContentModel.PROP_CONTENT.toPrefixString());
                    if (content == null) content = metadata.get(ContentModel.PROP_CONTENT.toString());
                    
                    File contentFile = null;
                    if (File.class.isInstance(content))
                    {
                        contentFile = File.class.cast(content);
                    }
                    else
                    {
                        contentFile = new File(content.toString());
                    }

                    contentFile = Tools.canonicalize(contentFile);
                    if (!contentFile.exists() || !contentFile.isFile() || !contentFile.canRead())
                    {
                        dryRun.addVersionFault(version, String.format("In-line content at [%s] is either missing, not a file, or not readable", contentFile.getPath()));
                        throw new DryRunException(dryRun);
                    }
                }

                importStatus.incrementTargetCounter(BulkImportStatus.TARGET_COUNTER_IN_PLACE_CONTENT_LINKED);
            }
            else  // Content needs to be streamed into the repository
            {
                if (trace(log)) trace(log, "Streaming content from '" + version.getContentSource() + "' into node '" + String.valueOf(nodeRef) + "'.");

                if (dryRun != null)
                {
                    try
                    {
                        version.validateContent();
                    }
                    catch (Exception e)
                    {
                        dryRun.addVersionFault(version, e);
                        throw new DryRunException(dryRun);
                    }
                }
                else
                {
                    ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
                    version.putContent(writer);
                }

                if (trace(log)) trace(log, "Finished streaming content from '" + version.getContentSource() + "' into node '" + String.valueOf(nodeRef) + "'.");

                importStatus.incrementTargetCounter(BulkImportStatus.TARGET_COUNTER_CONTENT_STREAMED);
            }
        }
    }

}
