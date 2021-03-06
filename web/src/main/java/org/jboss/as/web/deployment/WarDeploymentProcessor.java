/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.web.deployment;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.naming.Context;

import org.apache.catalina.Host;
import org.apache.catalina.Loader;
import org.apache.catalina.Realm;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.InstanceManager;
import org.jboss.as.ee.naming.ContextServiceNameBuilder;
import org.jboss.as.ee.naming.NamespaceSelectorService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.NamingListener;
import org.jboss.as.web.WebSubsystemElement;
import org.jboss.as.web.deployment.mock.MemoryRealm;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.vfs.VirtualFile;

/**
 * @author Emanuel Muckenhuber
 */
public class WarDeploymentProcessor implements DeploymentUnitProcessor {

    private final String defaultHost;

    public WarDeploymentProcessor(String defaultHost) {
        if(defaultHost == null) {
            throw new IllegalArgumentException("null default host");
        }
        this.defaultHost = defaultHost;
    }

    /** {@inheritDoc} */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if(metaData == null) {
            return;
        }
        Collection<String> hostNames = metaData.getMergedJBossWebMetaData().getVirtualHosts();
        if(hostNames == null || hostNames.isEmpty()) {
            hostNames = Collections.singleton(defaultHost);
        }
        for(final String hostName : hostNames) {
            if(hostName == null) {
                throw new IllegalStateException("null host name");
            }
            processDeployment(hostName, metaData, deploymentUnit, phaseContext.getServiceTarget());
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }

    protected void processDeployment(final String hostName, final WarMetaData warMetaData, final DeploymentUnit deploymentUnit, final ServiceTarget serviceTarget) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw new DeploymentUnitProcessingException("failed to resolve module for deployment " + deploymentRoot);
        }
        final ClassLoader classLoader = module.getClassLoader();
        final JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();

        // Create the context
        final StandardContext webContext = new StandardContext();
        final ContextConfig config = new JBossContextConfig(deploymentUnit);

        webContext.addInstanceListener(NamingListener.class.getName());

        // Set the deployment root
        try {
            webContext.setDocBase(deploymentRoot.getPhysicalFile().getAbsolutePath());
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
        webContext.addLifecycleListener(config);

        // Set the path name
        final String deploymentName = deploymentUnit.getName();
        String pathName = null;
        if (metaData.getContextRoot() == null) {
            pathName = deploymentRoot.getName();
            if (pathName.equals("ROOT.war")) {
                pathName = "";
            } else {
                pathName = "/" + pathName.substring(0, pathName.length() - 4);
            }
        } else {
            pathName = metaData.getContextRoot();
            if ("/".equals(pathName)) {
                pathName = "";
            }
        }
        webContext.setPath(pathName);
        webContext.setIgnoreAnnotations(true);

        // Add a dummy realm for now
        Realm realm = new MemoryRealm();
        webContext.setRealm(realm);

        //
        final Loader loader = new WebCtxLoader(classLoader);
        final InstanceManager manager = new WebInjectionContainer(classLoader);
        webContext.setInstanceManager(manager);
        webContext.setLoader(loader);

        // Set the session cookies flag according to metadata
        switch (metaData.getSessionCookies()) {
           case JBossWebMetaData.SESSION_COOKIES_ENABLED:
               webContext.setCookies(true);
               break;
           case JBossWebMetaData.SESSION_COOKIES_DISABLED:
               webContext.setCookies(false);
               break;
        }

        String metaDataSecurityDomain = metaData.getSecurityDomain();
        if (metaDataSecurityDomain != null) {
           metaDataSecurityDomain = metaDataSecurityDomain.trim();
        }

        //Add the namespace selector service
        ServiceName appNs = ContextServiceNameBuilder.app(deploymentUnit);
        ServiceName moduleNs = ContextServiceNameBuilder.module(deploymentUnit);
        ServiceName namespaceSelectorServiceName = deploymentUnit.getServiceName().append(NamespaceSelectorService.NAME);
        NamespaceSelectorService namespaceSelector = new NamespaceSelectorService();
        serviceTarget.addService(namespaceSelectorServiceName, namespaceSelector)
            .addDependency(appNs, Context.class,namespaceSelector.getApp())
            .addDependency(moduleNs, Context.class,namespaceSelector.getModule())
            .addDependency(moduleNs, Context.class,namespaceSelector.getComp())
            .install();

        // Add the context service
        try {
            WebDeploymentService webDeploymentService = new WebDeploymentService(webContext);
            serviceTarget.addService(WebSubsystemElement.JBOSS_WEB.append(deploymentName), webDeploymentService)
                .addDependency(WebSubsystemElement.JBOSS_WEB_HOST.append(hostName), Host.class, new WebContextInjector(webContext))
                .addDependency(namespaceSelectorServiceName, NamespaceSelectorService.class,webDeploymentService.getNamespaceSelector())
                .setInitialMode(Mode.ACTIVE)
                .install();
        } catch (ServiceRegistryException e) {
            throw new DeploymentUnitProcessingException("Failed to add JBoss web deployment service", e);
        }
    }

}
