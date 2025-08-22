package com.proxy.beautifier;
import java.nio.charset.Charset;
import com.proxy.utils.BodyType;

public interface Beautifier {
	
	/**
	* If this beautifier can handle this body type
	*
	* @param type the body type
	*/
	boolean accept(BodyType type);
	
	/**
	* format th content
	*
	* @param content the http body content
	* @param charset the charset of http body. now only for form-encoded content
	* @return the formatted text
	*/
	String beautify(String content, Charset charset);
}