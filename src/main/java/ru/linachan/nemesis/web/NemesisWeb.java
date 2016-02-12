package ru.linachan.nemesis.web;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import ru.linachan.nemesis.NemesisConfig;

public class NemesisWeb {

    private Server webServer;

    public NemesisWeb() {
        webServer = new Server(8484);

        ResourceHandler serverHandler = new ResourceHandler();

        serverHandler.setDirectoriesListed(true);
        serverHandler.setWelcomeFiles(new String[] { "index.html" });
        serverHandler.setResourceBase(NemesisConfig.getPath("logs").toString());

        GzipHandler gzip = new GzipHandler();
        webServer.setHandler(gzip);

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {serverHandler, new DefaultHandler() });
        gzip.setHandler(handlers);
    }

    public void start() throws Exception {
        webServer.start();
    }
}
