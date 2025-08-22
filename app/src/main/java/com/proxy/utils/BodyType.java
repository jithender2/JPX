package com.proxy.utils;

/**
 * `BodyType` is an enum representing the different types of message bodies that
 * can be handled by the application.  It categorizes body types into text-based,
 * image-based, and others (binary/unknown).  This categorization helps in
 * processing and handling different body types appropriately.
 */
public enum BodyType {

	text(0), html(0), xml(0), json(0), css(0), javascript(0), www_form(0),
	//TODO: multipart form encoded body
	jpeg(1), png(1), bmp(1), gif(1), icon(1), otherImage(1), binary(2), unknown(2);

	private static final long serialVersionUID = 1L;
	private final int type;

	BodyType(int type) {
		this.type = type;
	}

	public boolean isText() {
		return type == 0;
	}

	public boolean isImage() {
		return type == 1;
	}
}