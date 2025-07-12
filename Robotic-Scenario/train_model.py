import pandas as pd
import numpy as np
import tensorflow as tf
from sklearn.preprocessing import LabelEncoder, StandardScaler
from tensorflow.keras.utils import to_categorical
import joblib

# Load the data
file_path = 'smart_factory_with_collisions_100_robots.csv'
data = pd.read_csv(file_path, nrows=4_000_000)  # Load data for 40 robots

# Encode categorical columns
label_encoders = {}
if 'goal_status' in data.columns:
    le = LabelEncoder()
    data['goal_status'] = le.fit_transform(data['goal_status'])
    label_encoders['goal_status'] = le

    # Save the LabelEncoder for later use during inference
    label_encoder_path = 'label_encoder_goal_status.pkl'
    joblib.dump(le, label_encoder_path)
    print("LabelEncoder for 'goal_status' saved successfully.")


# One-hot encode the goal_status column for multi-class classification
goal_status_one_hot = to_categorical(data['goal_status'])

# Features and targets
features = ['current_time', 'px', 'py', 'pz', 'vx', 'vy']
targets = ['goal_status', 'idle', 'linear', 'rotational', 'Deadlock_Bool', 'RobotBodyContact']

# Sort by robotID and current_time
data = data.sort_values(by=['robotID', 'current_time'])

# Standardize features
scaler = StandardScaler()
data[features] = scaler.fit_transform(data[features])

# Save the fitted scaler
scaler_path = 'scaler.pkl'
joblib.dump(scaler, scaler_path)
print("Scaler saved successfully.")

# Parameters
sequence_length = 50  # Sequence length
batch_size = 32  # Batch size

# Generator for training sequences
def robot_data_generator(data, features, targets, sequence_length, batch_size, robot_ids):
    while True:
        X, y_goal_status, y_binary_targets = [], [], {f"{t}_output": [] for t in targets[1:]}
        for robot_id in robot_ids:
            group = data[data['robotID'] == robot_id]
            group_values = group[features].values
            binary_target_values = group[targets[1:]].values
            goal_status_values = goal_status_one_hot[group.index]
            for i in range(len(group) - sequence_length):
                X.append(group_values[i:i + sequence_length])  # Sequence of features
                y_goal_status.append(goal_status_values[i + sequence_length])  # Multi-class target
                for j, target in enumerate(targets[1:]):
                    y_binary_targets[f"{target}_output"].append(binary_target_values[i + sequence_length, j])  # Binary targets
                if len(X) == batch_size:
                    yield (
                        np.array(X, dtype=np.float32),
                        {
                            'goal_status_output': np.array(y_goal_status, dtype=np.float32),
                            **{k: np.array(v, dtype=np.float32) for k, v in y_binary_targets.items()}
                        }
                    )
                    X, y_goal_status, y_binary_targets = [], [], {f"{t}_output": [] for t in targets[1:]}  # Reset batch

# Split robotIDs into train and test
robot_ids = data['robotID'].unique()
train_robot_ids = robot_ids[:10]  # Use 10 robots for training
test_robot_ids = robot_ids[10:]   # Use the rest ofrobots for testing

# Create generators
train_generator = robot_data_generator(data, features, targets, sequence_length, batch_size, train_robot_ids)
test_generator = robot_data_generator(data, features, targets, sequence_length, batch_size, test_robot_ids)

# Steps per epoch
train_steps = sum(len(data[data['robotID'] == rid]) - sequence_length for rid in train_robot_ids) // batch_size
test_steps = sum(len(data[data['robotID'] == rid]) - sequence_length for rid in test_robot_ids) // batch_size

# Build the Multi-Output LSTM Model
input_layer = tf.keras.layers.Input(shape=(sequence_length, len(features)))

# Shared LSTM layer
lstm_layer = tf.keras.layers.LSTM(64, activation='tanh')(input_layer)

# Multi-class output for goal_status
goal_status_output = tf.keras.layers.Dense(
    goal_status_one_hot.shape[1], activation='softmax', name='goal_status_output')(lstm_layer)

# Binary outputs for other targets
binary_outputs = {
    f"{target}_output": tf.keras.layers.Dense(1, activation='sigmoid', name=f"{target}_output")(lstm_layer)
    for target in targets[1:]
}

# Define the model
model = tf.keras.Model(
    inputs=input_layer,
    outputs={'goal_status_output': goal_status_output, **binary_outputs}
)

# Compile the model
model.compile(
    optimizer='adam',
    loss={
        'goal_status_output': 'categorical_crossentropy',
        **{f"{target}_output": 'binary_crossentropy' for target in targets[1:]}
    },
    metrics={
        'goal_status_output': 'accuracy',
        **{f"{target}_output": 'accuracy' for target in targets[1:]}
    }
)

# Check output names for debugging
print("Model outputs:", model.output_names)

# Train the model
history = model.fit(
    train_generator,
    steps_per_epoch=train_steps,
    validation_data=test_generator,
    validation_steps=test_steps,
    epochs=1
)

# Evaluate the model
evaluation = model.evaluate(test_generator, steps=test_steps)
print("\nEvaluation Metrics:")
print(f"Goal Status Loss and Accuracy: {evaluation[:2]}")
for i, target in enumerate(targets[1:]):
    print(f"{target.capitalize()} Loss and Accuracy: {evaluation[2 + 2 * i:4 + 2 * i]}")

# Perform predictions on a subset of test data
test_robot_id = test_robot_ids[0]  # Pick the first robot from the test set
test_robot_data = data[data['robotID'] == test_robot_id]

# Create sequences for the selected robot
X_test_robot, y_test_robot = [], {'goal_status_output': [], **{f"{t}_output": [] for t in targets[1:]}}
group_values = test_robot_data[features].values
goal_status_values = goal_status_one_hot[test_robot_data.index]
binary_target_values = test_robot_data[targets[1:]].values

for i in range(len(test_robot_data) - sequence_length):
    X_test_robot.append(group_values[i:i + sequence_length])  # Sequence of features
    y_test_robot['goal_status_output'].append(goal_status_values[i + sequence_length])  # Multi-class target
    for j, target in enumerate(targets[1:]):
        y_test_robot[f"{target}_output"].append(binary_target_values[i + sequence_length, j])  # Binary targets

X_test_robot = np.array(X_test_robot, dtype=np.float32)
y_test_robot = {k: np.array(v, dtype=np.float32) for k, v in y_test_robot.items()}

# Make predictions
predictions = model.predict(X_test_robot)


# Save the model
model.save('smart_factory_model2.h5')
print("Model saved successfully.")
