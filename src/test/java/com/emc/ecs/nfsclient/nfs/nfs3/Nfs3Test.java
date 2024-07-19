package com.emc.ecs.nfsclient.nfs.nfs3;

import com.emc.ecs.nfsclient.nfs.io.Nfs3File;
import com.emc.ecs.nfsclient.rpc.CredentialUnix;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

public class Nfs3Test extends TestCase {

    @Test
    public void testBla() throws IOException {
        final Nfs3 nfs3 = new Nfs3("127.0.0.1", "/share", new CredentialUnix(0, 0, null), 3);

        final Nfs3File file = new Nfs3File(nfs3, "/");

        file.list().forEach(System.out::println);
    }

}

