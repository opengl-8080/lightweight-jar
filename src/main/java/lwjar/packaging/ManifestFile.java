package lwjar.packaging;

import lwjar.LightweightJarExecutor;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

class ManifestFile {
    private final ApplicationMainClass applicationMainClass;
    private final boolean springBoot;
    private final Charset charset;

    ManifestFile(ApplicationMainClass applicationMainClass, boolean springBoot, Charset charset) {
        this.applicationMainClass = Objects.requireNonNull(applicationMainClass);
        this.springBoot = springBoot;
        this.charset = Objects.requireNonNull(charset);
    }
    
    Manifest toManifest() {
        Manifest manifest = new Manifest();

        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, LightweightJarExecutor.class.getName());

        manifest.getMainAttributes().put(new Attributes.Name("Actual-Main-Class"), this.applicationMainClass.getName());
        manifest.getMainAttributes().put(new Attributes.Name("Is-Spring-Boot"), String.valueOf(this.springBoot));
        manifest.getMainAttributes().put(new Attributes.Name("Javac-Encoding"), this.charset.name());

        return manifest;
    }
}
