from flask import Flask, request
from flask_pymongo import PyMongo

from transformers import AutoTokenizer, AutoModelForCausalLM

# from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import torch
import json

tokenizer = AutoTokenizer.from_pretrained("microsoft/DialoGPT-medium")
model = AutoModelForCausalLM.from_pretrained("microsoft/DialoGPT-medium")
# tokenizer = AutoTokenizer.from_pretrained("facebook/blenderbot-400M-distill")
# model = AutoModelForSeq2SeqLM.from_pretrained("facebook/blenderbot-400M-distill")
# tokenizer = AutoTokenizer.from_pretrained("JosephusCheung/Guanaco")
# model = AutoModelForCausalLM.from_pretrained("JosephusCheung/Guanaco")
# tokenizer = AutoTokenizer.from_pretrained("PygmalionAI/pygmalion-6b")
# model = AutoModelForCausalLM.from_pretrained("PygmalionAI/pygmalion-6b")
# tokenizer = AutoTokenizer.from_pretrained("PygmalionAI/pygmalion-350m")
# model = AutoModelForCausalLM.from_pretrained("PygmalionAI/pygmalion-350m")

app = Flask(__name__)

mongodb_client = PyMongo(app, uri="mongodb://localhost:27017/Ariend")
db = mongodb_client.db


def ariend(username, user_input, messages=[], chat_history_ids=None):
    new_user_input_ids = tokenizer.encode(
        user_input + tokenizer.eos_token, return_tensors="pt"
    )

    bot_input_ids = (
        torch.cat([chat_history_ids, new_user_input_ids], dim=-1)
        if chat_history_ids is not None
        else new_user_input_ids
    )

    chat_history_ids = model.generate(
        bot_input_ids,
        max_length=1000,
        do_sample=True,
        temperature=0.2,
        pad_token_id=tokenizer.eos_token_id,
    )

    response = tokenizer.decode(
        chat_history_ids[:, bot_input_ids.shape[-1] :][0], skip_special_tokens=True
    )

    messages.append(user_input)
    messages.append(response)

    db.users.update_one(
        {"username": username},
        {"$set": {"messages": messages, "history": toString(chat_history_ids)}},
    )

    return response


def toString(tensor):
    tensor_as_list = tensor.tolist()
    tensor_as_json = json.dumps(tensor_as_list)
    return tensor_as_json


def toTensor(string):
    restored_list = json.loads(string)
    restored_tensor = torch.tensor(restored_list)
    return restored_tensor


@app.route("/login", methods=["POST"])
def login():
    data = db.users.find_one(
        {"username": request.json["username"], "password": request.json["password"]}
    )
    if data is not None:
        return {"res": data["messages"]}, 200
    else:
        return {"res": []}, 400


@app.route("/register", methods=["POST"])
def register():
    if db.users.find_one({"username": request.json["username"]}) is None:
        if (
            db.users.insert_one(
                {
                    "username": request.json["username"],
                    "password": request.json["password"],
                    "messages": [],
                    "history": "",
                }
            )
            is not None
        ):
            return {"res": "1"}, 200
        else:
            return {"res": "0"}, 400
    else:
        return {"res": "0"}, 400


@app.route("/message", methods=["POST"])
def message():
    data = db.users.find_one({"username": request.json["username"]})
    if data["history"] != "":
        ariend_response = ariend(
            request.json["username"],
            request.json["message"],
            data["messages"],
            toTensor(data["history"]),
        )
        print(ariend_response)
        return {"res": ariend_response}, 200
    else:
        ariend_response = ariend(request.json["username"], request.json["message"])
        print(ariend_response)
        return {"res": ariend_response}, 200


@app.route("/reset", methods=["POST"])
def reset():
    db.users.update_one(
        {"username": request.json["username"]},
        {"$set": {"messages": [], "history": ""}},
    )
    return {"res": "1"}, 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
