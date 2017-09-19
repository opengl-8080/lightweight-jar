package lwjar;

import java.nio.charset.Charset;

public class GlobalOption {
    private static Charset charset;
    
    static void setEncoding(Charset charset) {
        GlobalOption.charset = charset;
    }
    
    public static Charset getEncoding() {
        return GlobalOption.charset;
    }
}
