/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.osgi.serviceloader.internal;

//$Id$

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;

import org.jboss.osgi.deployment.interceptor.AbstractLifecycleInterceptor;
import org.jboss.osgi.deployment.interceptor.InvocationContext;
import org.jboss.virtual.VirtualFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interceptor that registeres service defined in META-INF/services.
 * 
 * @author thomas.diesler@jboss.com
 * @since 26-Jan-2010
 */
public class ServiceLoaderInterceptor extends AbstractLifecycleInterceptor
{
   // Provide logging
   private Logger log = LoggerFactory.getLogger(ServiceLoaderInterceptor.class);

   @SuppressWarnings("unchecked")
   public void invoke(int state, InvocationContext context)
   {
      // Parse and create metadta on STARTING
      if (state == Bundle.STARTING)
      {
         Bundle bundle = context.getBundle();
         if (bundle.getEntry("META-INF/services/") == null)
            return;

         Enumeration<String> entryPaths = bundle.getEntryPaths("META-INF/services/");
         while (entryPaths.hasMoreElements())
         {
            String entryPath = entryPaths.nextElement();
            if (entryPath.endsWith("/") == false)
               processServicesEntryPath(context, entryPath);
         }
      }
   }

   private void processServicesEntryPath(InvocationContext context, String entryPath)
   {
      Bundle bundle = context.getBundle();
      VirtualFile root = context.getRoot();

      try
      {
         String serviceName = entryPath.substring("META-INF/services/".length());
         Class<?> serviceClass = bundle.loadClass(serviceName);

         VirtualFile child = root.getChild(entryPath);
         InputStream inStream = child.openStream();

         BufferedReader br = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
         String implClassName = br.readLine();
         while (implClassName != null)
         {
            int hashIndex = implClassName.indexOf("#");
            if (hashIndex > 0)
               implClassName = implClassName.substring(0, hashIndex);

            implClassName = implClassName.trim();

            if (implClassName.length() > 0)
            {
               // Load the service class
               Class<?> implClass = (Class<?>)bundle.loadClass(implClassName);
               if (serviceClass.isAssignableFrom(implClass) == false)
               {
                  log.warn("Not assignable: " + implClassName);
                  continue;
               }

               // Provide service properties
               Hashtable<String, String> props = new Hashtable<String, String>();
               props.put(Constants.VERSION_ATTRIBUTE, bundle.getVersion().toString());
               String vendor = (String)bundle.getHeaders().get(Constants.BUNDLE_VENDOR);
               props.put(Constants.SERVICE_VENDOR, (vendor != null ? vendor : "anonymous"));

               // Register the service factory on behalf of the intercepted bundle 
               MetaInfServiceFactory factory = new MetaInfServiceFactory(implClass);
               BundleContext bundleContext = bundle.getBundleContext();
               bundleContext.registerService(serviceName, factory, props);
            }

            implClassName = br.readLine();
         }
         br.close();
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception ex)
      {
         log.error("Cannot process: " + entryPath, ex);
      }
   }

   static class MetaInfServiceFactory implements ServiceFactory
   {
      private Class<?> serviceClass;

      public MetaInfServiceFactory(Class<?> serviceClass)
      {
         this.serviceClass = serviceClass;
      }

      public Object getService(Bundle bundle, ServiceRegistration registration)
      {
         try
         {
            Object serviceInstance = serviceClass.newInstance();
            return serviceInstance;
         }
         catch (Exception ex)
         {
            throw new IllegalStateException("Cannot instanciate service", ex);
         }
      }

      public void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
      {
      }
   }
}
