
import matplotlib.pyplot as plt

data = []
with open("ExceptionVsResult.csv", "r") as fin:
    lines = fin.readlines()
    for line in lines[1:]:
        parts = line.split(",")
        data.append({
            'p': float(parts[0]),
            'exMin': int(parts[1]),
            'exMax': int(parts[2]),
            'exAvg': int(parts[3]),
            'resMin': int(parts[4]),
            'resMax': int(parts[5]),
            'resAvg': int(parts[6])
        })

data.sort(key=lambda entry: entry['p'])
prob = [d['p'] for d in data]

plt.plot(prob, [d['exAvg'] for d in data], 'b', label="Exceptions Average")
# plt.plot(prob, [d['exMin'] for d in data], 'b:', label="Exceptions Min")
# plt.plot(prob, [d['exMax'] for d in data], 'b:', label="Exceptions Max")
plt.plot(prob, [d['resAvg'] for d in data], 'r', label="Results Average")
# plt.plot(prob, [d['resMin'] for d in data], 'r:', label="Results Min")
# plt.plot(prob, [d['resMax'] for d in data], 'r:', label="Results Max")
plt.xlabel("Error Probability")
plt.ylabel("Runtime")
plt.title("`parseInt` runtime for increasing probability of bad strings")
plt.legend()
plt.show()
