package com.proxy.beautifier;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.proxy.utils.BodyType;
import java.util.Map;
import java.util.stream.Collectors;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

public class JsonBeautifier implements Beautifier {
	private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

	@Override
	public boolean accept(BodyType type) {
		return type == BodyType.json;
	}

	@Override
	public String beautify(String content, Charset charset) {

		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Enable pretty-printing
			JsonElement jsonElement = JsonParser.parseString(content);
			return gson.toJson(jsonElement);
		} catch (Exception e) {
			return content; // Return original content if parsing fails
		}

	}

}