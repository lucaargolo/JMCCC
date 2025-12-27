package org.to2mbn.jmccc.version;

import java.io.Serializable;
import java.util.Objects;

public class JavaVersionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String component;
    private int majorVersion;

    /**
     * Constructor of JavaVersion.
     *
     * @param component    the java version component
     * @param majorVersion the java major version
     */
    public JavaVersionInfo(String component, int majorVersion) {
        this.component = component;
        this.majorVersion = majorVersion;
    }

    /**
     * Gets the java version component
     *
     * @return the java version component
     */
    public String getComponent() {
        return component;
    }

    /**
     * Gets the java major version
     *
     * @return the java major version
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(component, majorVersion);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof JavaVersionInfo) {
            JavaVersionInfo another = (JavaVersionInfo) obj;
            return Objects.equals(component, another.component)
                    && majorVersion == another.majorVersion;
        }
        return false;
    }

}
