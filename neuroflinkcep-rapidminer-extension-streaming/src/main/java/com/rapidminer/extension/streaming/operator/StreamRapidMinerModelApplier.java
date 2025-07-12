/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.rapidminer.example.Attribute;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.raap.DefaultSettingValue;
import com.rapidminer.extension.streaming.raap.bridge.DataTypeConverter;
import com.rapidminer.extension.streaming.raap.bridge.TypeConversionException;
import com.rapidminer.extension.streaming.raap.data.DataType;
import com.rapidminer.extension.streaming.utility.ResourceManager;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.transform.RapidMinerModelApplierTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.Transformer;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.tools.IOObjectSerializer;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.repository.IOObjectEntry;


/**
 * RapidMiner model applier for stream graphs
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class StreamRapidMinerModelApplier extends AbstractStreamTransformOperator {

	private static final String MODEL_FILE_NAME_PREFIX = "rm-model-";

	private static final String MODEL_FILE_NAME_SUFFIX = IOObjectEntry.IOO_SUFFIX;

	private static final String PARAMETER_RAPIDMINER_HOME = "rapidminer_home";

	private static final String PARAMETER_RAPIDMINER_USER_HOME = "rapidminer_user_home";

	private final InputPort modelInput = getInputPorts().createPort("model", Model.class);

	public StreamRapidMinerModelApplier(OperatorDescription description) {
		super(description);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType key = new ParameterTypeString(
			PARAMETER_RAPIDMINER_HOME,
			"Path to RapidMiner Home on the cluster side. (advice: don't change it, depends on the cluster setup)",
			DefaultSettingValue.RM_HOME,
			true);
		types.add(key);

		ParameterType value = new ParameterTypeString(
			PARAMETER_RAPIDMINER_USER_HOME,
			"Path to RapidMiner User-Home on the cluster side. (advice: don't change it, depends on the cluster " +
				"setup)",
			DefaultSettingValue.RM_USER_HOME,
			true);
		types.add(value);

		return types;
	}

	@Override
	public Transformer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
		try {
			Path tempModelPath = ResourceManager.createTempFile(MODEL_FILE_NAME_PREFIX, MODEL_FILE_NAME_SUFFIX);
			Model model = modelInput.getData(Model.class);
			Map<String, DataType> dataTypes = getFeatureTypes(model);

			// Serialize model and add it to the artifacts (that will be embedded)
			serializeModel(tempModelPath, model);
			graph.getArtifacts().add(tempModelPath.toString());

			return new RapidMinerModelApplierTransformer.Builder(graph)
				.withFeatures(dataTypes)
				.withModelFileName(tempModelPath.toFile().getName())
				.withParent(inData.getLastNode())
				.withRMHome(getParameterAsString(PARAMETER_RAPIDMINER_HOME))
				.withRMUserHome(getParameterAsString(PARAMETER_RAPIDMINER_USER_HOME))
				.build();
		} catch (IOException e) {
			throw new UserError(this, e, 303,
				MODEL_FILE_NAME_PREFIX + "*****" + MODEL_FILE_NAME_SUFFIX, e.getMessage());
		} catch (TypeConversionException e) {
			throw new UserError(this, e, "raap.invalid_type", e.getMessage());
		}
	}

	/**
	 * Gets the features and attributes from the model using its HeaderExampleSet
	 *
	 * @param model
	 * 	to source from
	 * @return mapping between features and data-types
	 * @throws TypeConversionException
	 * 	if the RapidMiner type is not RaaP-acceptable
	 */
	private Map<String, DataType> getFeatureTypes(Model model) throws TypeConversionException {
		Map<String, DataType> result = Maps.newHashMap();
		Iterator<Attribute> attrIterator = model.getTrainingHeader().getAttributes().iterator();

		while (attrIterator.hasNext()) {
			Attribute attribute = attrIterator.next();
			result.put(attribute.getName(), DataTypeConverter.getDataType(attribute.getValueType()));
		}

		return result;
	}

	/**
	 * Serializes the model into the target file
	 *
	 * @param target
	 * 	path to the file into which the model will be placed
	 * @param model
	 * 	to be serialized
	 */
	private void serializeModel(Path target, Model model) throws IOException {
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(target.toFile()))) {
			IOObjectSerializer.getInstance().serialize(out, model);
		}
	}

}