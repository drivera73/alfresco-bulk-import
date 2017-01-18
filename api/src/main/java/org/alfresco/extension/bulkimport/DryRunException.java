package org.alfresco.extension.bulkimport;

public class DryRunException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private final DryRun<?> dryRun;

	public DryRunException(DryRun<?> dryRun) {
		this.dryRun = dryRun;
	}
	
	public DryRun<?> getDryRun()
	{
		return this.dryRun;
	}
}