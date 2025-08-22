package com.proxy.setting;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.file.Path;

public class Settings {

	public static final int rootCertValidityDays = 3650;

	public static final int certValidityDays = 10;
	public static final char[] rootKeyStorePassword = "JPXP4SSP0WRD00".toCharArray();
	public static final char[] keyStorePassword = "JPXP4SSP0WRD00".toCharArray();		
	public static final String certAliasName = "JPX app";

	
	
}