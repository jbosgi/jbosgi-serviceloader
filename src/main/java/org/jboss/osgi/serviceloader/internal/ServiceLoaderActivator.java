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

import org.jboss.osgi.deployment.interceptor.LifecycleInterceptor;
import org.jboss.osgi.serviceloader.ServiceLoaderService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * A bundle activator that registers the {@link ServiceLoaderInterceptor}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 26-Jan-2010
 */
public class ServiceLoaderActivator implements BundleActivator
{
   public void start(BundleContext context)
   {
      // Register marker service
      context.registerService(ServiceLoaderService.class.getName(), new ServiceLoaderService(){}, null);
      
      // Register lifecycle interceptor
      ServiceLoaderInterceptor interceptor = new ServiceLoaderInterceptor();
      context.registerService(LifecycleInterceptor.class.getName(), interceptor, null);
   }

   public void stop(BundleContext context)
   {
   }
}