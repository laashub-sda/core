/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.classloader;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.classloader.mock.collisions.ClassCreatesInstanceFromClassLoader;
import org.jboss.forge.classloader.mock.collisions.ClassImplementsInterfaceExtendsInterfaceValue;
import org.jboss.forge.classloader.mock.collisions.ClassImplementsInterfaceModifiableContext;
import org.jboss.forge.classloader.mock.collisions.ClassImplementsInterfaceWithArrayParameterModification;
import org.jboss.forge.classloader.mock.collisions.ClassImplementsInterfaceWithGetterAndSetter;
import org.jboss.forge.classloader.mock.collisions.ClassImplementsInterfaceWithPassthroughMethod;
import org.jboss.forge.classloader.mock.collisions.ClassWithGetterAndSetter;
import org.jboss.forge.classloader.mock.collisions.ClassWithPassthroughMethod;
import org.jboss.forge.classloader.mock.collisions.InterfaceValue;
import org.jboss.forge.classloader.mock.collisions.InterfaceWithGetterAndSetter;
import org.jboss.forge.classloader.mock.collisions.InterfaceWithPassthroughMethod;
import org.jboss.forge.container.addons.AddonId;
import org.jboss.forge.container.addons.AddonRegistry;
import org.jboss.forge.container.repositories.AddonDependencyEntry;
import org.jboss.forge.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.proxy.Proxies;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ClassLoaderAdapterPassthroughTest
{
   @Deployment(order = 3)
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create(AddonId.from("dep", "1"))
               );

      return archive;
   }

   @Deployment(name = "dep,1", testable = false, order = 1)
   public static ForgeArchive getDeploymentDep2()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addPackages(true, ClassWithGetterAndSetter.class.getPackage())
               .addBeansXML();

      return archive;
   }

   @Inject
   private AddonRegistry registry;

   @Test
   public void testParameterTypeCollision() throws Exception
   {
      ClassLoader thisLoader = ClassLoaderAdapterPassthroughTest.class.getClassLoader();
      ClassLoader loader1 = registry.getAddon(AddonId.from("dep", "1")).getClassLoader();

      ClassWithGetterAndSetter local = new ClassWithGetterAndSetter();
      local.setPassthrough((ClassWithPassthroughMethod) loader1
               .loadClass(ClassWithPassthroughMethod.class.getName())
               .newInstance());

      Object delegate = loader1.loadClass(ClassWithGetterAndSetter.class.getName()).newInstance();
      ClassWithGetterAndSetter enhanced = (ClassWithGetterAndSetter) ClassLoaderAdapterBuilder
               .callingLoader(thisLoader).delegateLoader(loader1).enhance(delegate);

      enhanced.setPassthrough(new ClassWithPassthroughMethod());

      Assert.assertNotNull(enhanced);
      Assert.assertNotNull(enhanced.getPassthrough());
      Assert.assertFalse(Proxies.isForgeProxy(enhanced.getPassthrough()));
      Assert.assertFalse(enhanced.assertPassthroughNotProxied());
   }
}
