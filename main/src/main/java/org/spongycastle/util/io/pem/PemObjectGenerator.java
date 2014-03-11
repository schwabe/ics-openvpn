package org.spongycastle.util.io.pem;

public interface PemObjectGenerator
{
    PemObject generate()
        throws PemGenerationException;
}
