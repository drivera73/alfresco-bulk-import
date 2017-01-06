package org.alfresco.extension.bulkexport.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.alfresco.service.cmr.repository.ContentIOException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Checksum {
	protected static Collection<String> DEFAULT_HASH_ALGOS = Arrays
			.asList(new String[] { "MD5", "SHA-1", "SHA-256", "SHA-512" });
	protected static Collection<String> ALLOWED_HASH_ALGOS = DEFAULT_HASH_ALGOS;

	private static Log logger = LogFactory.getLog(Checksum.class);

	public static String calculateChecksum(InputStream data, long size, String sum) {
		String rv = null;

		if (!ALLOWED_HASH_ALGOS.contains(sum)) {
			logger.info("Unsupported sum mode: " + sum);
			throw new IllegalArgumentException("Unsupported sum '" + sum + "'");
		}

		try {
			String hex = sum + ":" + size + ":";
			switch (sum) {
			case "MD5":
				hex = hex + DigestUtils.md5Hex(data);
				break;
			case "SHA-1":
				hex = hex + DigestUtils.sha1Hex(data);
				break;
			case "SHA-256":
				hex = hex + DigestUtils.sha256Hex(data);
				break;
			case "SHA-512":
				hex = hex + DigestUtils.sha512Hex(data);
				break;
			}
			
			data.close();
			rv = hex;
		} catch (ContentIOException e) {
			try {
				throw new IOException("IO Exception calculating hashes", e);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return rv;
	}
}
