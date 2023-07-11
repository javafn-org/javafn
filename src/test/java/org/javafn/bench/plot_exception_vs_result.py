
import matplotlib.pyplot as plt

data = []
with open("ExceptionVsResult.csv", "r") as fin:
    lines = fin.readlines()
    for line in lines[1:]:
        parts = line.split(",")
        data.append((float(parts[0]), int(parts[1]), int(parts[2])))

data.sort()

plt.plot([p for p, _, _ in data], [e for _, e, _ in data], label="Exceptions")
plt.plot([p for p, _, _ in data], [r for _, _, r in data], label="Results")
plt.xlabel("Error Probability")
plt.ylabel("Runtime")
plt.legend()
plt.show()
