/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.util;


/**
 * Syntactic sugar for Thread context class-loading swap
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public final class ClassLoaderSwapper implements AutoCloseable {

	private final ClassLoader oldCL;

	public static ClassLoaderSwapper withContextClassLoader(ClassLoader newClassLoader) {
		return new ClassLoaderSwapper(newClassLoader);
	}

	private ClassLoaderSwapper(ClassLoader newCL) {
		this.oldCL = Thread.currentThread().getContextClassLoader();

		Thread.currentThread().setContextClassLoader(newCL);
	}

	@Override
	public void close() {
		Thread.currentThread().setContextClassLoader(this.oldCL);
	}

}