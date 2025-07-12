import pandas as pd

# Load the Excel file
file_path = "stats_xlsx/stats_2047_avg.xlsx"  # Change this to your actual file path
df = pd.read_excel(file_path, index_col=0)  # Assuming first column is row labels

# Replace -1 with NaN to compute row-wise mean correctly
df.replace(-1, pd.NA, inplace=True)

# Compute row-wise mean, ignoring NaN values
row_means = df.mean(axis=1, skipna=True)

# Fill missing values (-1 replaced with NaN) with the row-wise mean
df = df.apply(lambda row: row.fillna(row_means[row.name]), axis=1)

# Save the cleaned data back to Excel
output_path = "stats_xlsx/stats_2047_avg_clean.xlsx"
df.to_excel(output_path)

print(f"Processed file saved as: {output_path}")
