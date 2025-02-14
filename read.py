import re

input_file = "out1.txt"
output_file = "filtered_output.txt"

pattern = re.compile(r"^\d+\s*-\s*\d+\s*:\s*\d+\.\d+$")

with open(input_file, "r") as infile, open(output_file, "w") as outfile:
    for line in infile:
        if not pattern.match(line.strip()):
            outfile.write(line)
