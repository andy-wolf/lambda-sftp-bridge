package de.andywolf.sftpbridge;

import de.andywolf.sftpbridge.base.Connection;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents options to use when creating a {@link Connection connection}.
 */
@Slf4j
@NoArgsConstructor
@EqualsAndHashCode
public class ConnectionOptions {

    private static final Set<String> filteredKeys = new HashSet<>();

    public static String registerFilteredKey(String key) {
        filteredKeys.add(key);
        return key;
    }

    public static final String HEARTBEAT_INTERVAL = "heartbeatInterval";
    public static final int HEARTBEAT_INTERVAL_DEFAULT = 0;

    public static final String PASSPHRASE = registerFilteredKey("passphrase");
    public static final String PRIVATE_KEY = registerFilteredKey("privateKey");

    public static final String CONNECTION_TIMEOUT_MILLIS = "connectionTimeoutMillis";
    public static final int CONNECTION_TIMEOUT_MILLIS_DEFAULT = 120000;

	public static final String SOCKET_TIMEOUT_MILLIS = "socketTimeoutMillis";
	public static final int SOCKET_TIMEOUT_MILLIS_DEFAULT = 0;

    public static final String ADDRESS = "address";

    public static final String PORT = "port";
    public static final int PORT_DEFAULT_SSH = 22;

    public static final String USERNAME = "username";
    public static final String PASSWORD = registerFilteredKey("password");

    public static final String REMOTE_COPY_BUFFER_SIZE = "remoteCopyBufferSize";
    public static final int REMOTE_COPY_BUFFER_SIZE_DEFAULT = 64 * 1024; // 64 KB

    public static final String ENDPOINT_URL = "endpointURL";
    public static final String ENDPOINT_URL_DEFAULT = "https://s3.eu-central-1.amazonaws.com";

    public static final String SIGNING_REGION = "signingRegion";
    public static final String SIGNING_REGION_DEFAULT = "eu-central-1";


    private final Map<String, Object> options = new HashMap<>();

    /**
     * Sets a connection option.
     *
     * @param key   the key of the connection option.
     * @param value the value of the connection option.
     */
    public void set(String key, Object value) {
        options.put(key, value);
    }

    /**
     * Retrieves the value of a required connection option.
     *
     * @param <T> the type of the connection option.
     * @param key the key of the connection option.
     * @return the value of the connection option.
     * @throws IllegalArgumentException if no value was supplied for the connection option
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) throws IllegalArgumentException {
        T value = (T) options.get(key);
        if (value == null) {
            throw new IllegalArgumentException("No value specified for required connection option " + key);
        }
        return value;
    }

    /**
     * Retrieves the value of an optional connection option.
     *
     * @param <T> the type of the connection option.
     * @param key the key of the connection option.
     * @return the value of the connection option or <code>null</code> if that option was not specified.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOptional(String key) {
        return (T) options.get(key);
    }

    /**
     * Retrieves the value of a connection option or a default value if that option has not been set.
     *
     * @param <T>          the type of the connection option.
     * @param key          the key of the connection option.
     * @param defaultValue the default value to use of the connection options has not been set.
     * @return the value of the connection option or the default value if that option was not specified.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        if (options.containsKey(key)) {
            return (T) options.get(key);
        } else {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key) {
        Object o = options.get(key);
        if (o == null) {
            throw new IllegalArgumentException("No value specified for required connection option " + key);
        } else if (o instanceof Boolean) {
            return (Boolean) o;
        } else if (o instanceof String) {
            return Boolean.valueOf((String) o);
        } else {
            throw new IllegalArgumentException("Value specified for required connection option " + key + " is neither a Boolean nor a String");
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object o = options.get(key);
        if (o == null) {
            return defaultValue;
        } else if (o instanceof Boolean) {
            return (Boolean) o;
        } else if (o instanceof String) {
            return Boolean.valueOf((String) o);
        } else {
            throw new IllegalArgumentException("Value specified for connection option " + key + " is neither a Boolean nor a String");
        }
    }

    public int getInteger(String key) {
        Object o = options.get(key);
        if (o == null) {
            throw new IllegalArgumentException("No value specified for required connection option " + key);
        } else if (o instanceof Integer) {
            return (Integer) o;
        } else if (o instanceof String) {
            return Integer.parseInt((String) o);
        } else {
            throw new IllegalArgumentException("Value specified for required connection option " + key + " is neither an Integer nor a String");
        }
    }

    public int getInteger(String key, int defaultValue) {
        Object o = options.get(key);
        if (o == null) {
            return defaultValue;
        } else if (o instanceof Integer) {
            return (Integer) o;
        } else if (o instanceof String) {
            return Integer.parseInt((String) o);
        } else {
            throw new IllegalArgumentException("Value specified for connection option " + key + " is neither an Integer nor a String");
        }
    }

    public <T extends Enum<T>> T getEnum(String key, Class<T> enumClazz) {
        T o = getEnum(key, enumClazz, null);
        if (o == null) {
            throw new IllegalArgumentException("No value specified for required connection option " + key);
        } else {
            return o;
        }
    }

    public <T extends Enum<T>> T getOptionalEnum(String key, Class<T> enumClazz) {
        return getEnum(key, enumClazz, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T getEnum(String key, Class<T> enumClazz, T defaultValue) {
        Object o = options.get(key);
        if (o == null) {
            return defaultValue;
        } else if (o.getClass().equals(enumClazz)) {
            return (T) o;
        } else if (o instanceof String) {
            return Enum.valueOf(enumClazz, (String) o);
        } else {
            throw new IllegalArgumentException("Value specified for connection option " + key + " is neither an instanceof of " + enumClazz.getName()
                    + " nor a String");
        }
    }

    /**
     * Returns whether a connection option is set.
     *
     * @param key the key of the connection option.
     * @return true iff the connection option is set, false otherwise.
     */
    public boolean containsKey(String key) {
        return options.containsKey(key);
    }

    /**
     * Returns the keys of all connection options set.
     *
     * @return a {@link Set} containing the keys.
     */
    public Set<String> keys() {
        return options.keySet();
    }

    @Override
    public String toString() {
        return print(this, "");
    }

    private static String print(ConnectionOptions options, String indent) {
        StringBuilder b = new StringBuilder();
        b.append("ConnectionOptions[\n");
        for (Map.Entry<String, Object> e : options.options.entrySet()) {
            b.append(indent).append("\t").append(e.getKey()).append(" --> ");
            Object value = e.getValue();
            if (value instanceof ConnectionOptions) {
                b.append(print((ConnectionOptions) value, indent + "\t"));
            } else {
                b.append(filteredKeys.contains(e.getKey()) ? "********" : value);
            }
            b.append("\n");
        }
        b.append(indent).append("]");
        return b.toString();
    }
}
