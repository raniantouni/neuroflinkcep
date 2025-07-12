import tensorflow as tf
from tensorflow.keras.models import load_model

h5_model_path = "smart_factory_model2.h5"

# Load the model after fixing encoding issues
model = load_model(h5_model_path)

print(model)

model.save("./model", save_format="tf")
