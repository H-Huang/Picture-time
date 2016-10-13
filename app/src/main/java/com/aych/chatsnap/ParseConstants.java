package com.aych.chatsnap;

/**
 * Created by HowardHuang on 7/6/2015.
 */
public final class ParseConstants {
    //final means it wont change at all
    //class name
    public static final String CLASS_MESSAGES = "Messages";

    //field name
    //static means you dont need an instance of parse constants
    public static final String KEY_USERNAME = "username";
    public static final String KEY_FRIENDS_RELATION = "friendsRelation";
    public static final String KEY_RECIPIENTS_IDS = "recipientsIds";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_SENDER_NAME = "senderName";
    public static final String KEY_FILE = "file";
    public static final String KEY_FILE_TYPE = "fileType";
    public static final String KEY_CREATED_AT = "createdAt";

    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_VIDEO = "video";
}
