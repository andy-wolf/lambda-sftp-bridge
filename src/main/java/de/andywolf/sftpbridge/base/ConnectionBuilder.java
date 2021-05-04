package de.andywolf.sftpbridge.base;

public interface ConnectionBuilder {

    Connection build();

    ConnectionBuilder withOption(String key, String value);

    ConnectionBuilder withOption(String key, int value);
}
