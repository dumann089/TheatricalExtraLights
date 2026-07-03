package org.qualet.irl.patcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Thin wrapper over the host's shaderpack directory + listing, with a direct-scan fallback. */
public final class Shaderpacks
{
    private static final Logger LOG = LoggerFactory.getLogger("irl-patcher");

    private Shaderpacks()
    {}

    public static Path dir()
    {
        PatcherHost host = Patcher.host();
        try
        {
            Path p = host.shaderpacksDir();
            if (p != null)
            {
                return p;
            }
        }
        catch (Throwable t)
        {
            LOG.warn("host.shaderpacksDir failed: {}", t.toString());
        }

        return host.gameDir().resolve("shaderpacks");
    }

    public static List<String> list()
    {
        Set<String> names = new LinkedHashSet<>();

        try
        {
            names.addAll(Patcher.host().listShaderpacks());
        }
        catch (Throwable t)
        {
            LOG.warn("host.listShaderpacks failed: {}", t.toString());
        }

        if (names.isEmpty())
        {
            Path dir = dir();
            try (Stream<Path> stream = Files.list(dir))
            {
                stream.forEach(p ->
                {
                    String name = p.getFileName().toString();
                    if (Files.isDirectory(p) || name.toLowerCase().endsWith(".zip"))
                    {
                        names.add(name);
                    }
                });
            }
            catch (Throwable t)
            {
                LOG.warn("Shaderpack dir scan failed for {}: {}", dir, t.toString());
            }
        }

        LOG.info("Shaderpacks: dir={} count={}", dir(), names.size());
        return new ArrayList<>(names);
    }

    public static Path packPath(String name)
    {
        return dir().resolve(name);
    }

    public static void openFolder()
    {
        Patcher.host().openFolder(dir());
    }
}
