/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.crexdata.JsonDataProducer;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisDataProducer;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisEstimateQuery;
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.extension.streaming.utility.graph.transform.*;
import com.rapidminer.extension.streaming.utility.graph.source.SpringFinancialSource;


/**
 * Utility for serializing/de-serializing objects with Kryo. NOT thread-safe!
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class KryoHandler {

	private final Kryo kryo = new Kryo();

	public KryoHandler() {
		initialize();
	}

	/**
	 * Reads input stream and de-serializes its content into a 'clazz' instance
	 *
	 * @param inputStream
	 * @param clazz
	 * @param <T>
	 * @return object from input stream
	 */
	public <T> T read(InputStream inputStream, Class<T> clazz) {
		try (Input input = new Input(inputStream)) {
			return kryo.readObject(input, clazz);
		}
	}

	/**
	 * Reads 'src' file and de-serializes its content into a 'clazz' instance
	 *
	 * @param src
	 * @param clazz
	 * @param <T>
	 * @return object from file
	 * @throws FileNotFoundException
	 */
	public <T> T read(File src, Class<T> clazz) throws FileNotFoundException {
		return read(new FileInputStream(src), clazz);
	}

	/**
	 * Writes serialized object into the output stream
	 *
	 * @param outputStream
	 * @param obj
	 */
	public void write(OutputStream outputStream, Object obj) {
		try (Output output = new Output(outputStream)) {
			kryo.writeObject(output, obj);
		}
	}

	/**
	 * Writes serialized object into 'dst' file
	 *
	 * @param dst
	 * @param obj
	 * @throws FileNotFoundException
	 */
	public void write(File dst, Object obj) throws FileNotFoundException {
		write(new FileOutputStream(dst), obj);
	}

	/**
	 * Registers class for serialization/deserialization
	 *
	 * @param clazz
	 * @param <T>
	 */
	public <T> void register(Class<T> clazz) {
		kryo.register(clazz);
	}

	/**
	 * Initializes this handler instance: registers classes
	 */
	private void initialize() {
		kryo.register(StreamGraph.class);

		kryo.register(KafkaSink.class);
		kryo.register(KafkaSource.class);
		kryo.register(AggregateTransformer.class);
		kryo.register(DuplicateStreamTransformer.class);
		kryo.register(FilterTransformer.class);
		kryo.register(JoinTransformer.class);
		kryo.register(MapTransformer.class);
		kryo.register(SelectTransformer.class);
		kryo.register(ConnectTransformer.class);
		kryo.register(ParseFieldTransformer.class);
		kryo.register(StringifyFieldTransformer.class);
		kryo.register(UnionTransformer.class);
		kryo.register(CEP.class);
		kryo.register(JsonDataProducer.class);


		// INFORE
		kryo.register(SynopsisEstimateQuery.class);
		kryo.register(SynopsisDataProducer.class);

		kryo.register(SpringFinancialSource.class);
	}

}