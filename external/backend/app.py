from flask import Flask, request, jsonify

app = Flask(__name__)

def fibonacci(n):
    if n <= 1:
        return n
    return fibonacci(n - 1) + fibonacci(n - 2)

@app.route("/fibonacci")
def calculate():
    try:
        n = int(request.args.get("n", 20))
        result = fibonacci(n)
        return jsonify({"result": result})
    except Exception as e:
        return jsonify({"error": str(e)}), 400

@app.route("/")
def health():
    return "OK", 200

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=80)
