#!/usr/bin/python3

import sqlite3
import pprint
from bs4 import BeautifulSoup as soup
from urllib.request import urlopen as u_req
import pprint
import json
import sys
import math
import uuid
from datetime import date
import psycopg2 as pgsql

# Initialize pretty printer
pp = pprint.PrettyPrinter(indent = 4)

def create_table():
    # Connecting to sqlite
    connection_obj = sqlite3.connect('jeopardy_clues.db')
    cursor_obj = connection_obj.cursor()

    # Drop the GEEK table if already exists.
    cursor_obj.execute("DROP TABLE IF EXISTS CLUES")

    # Creating table
    table = """ CREATE TABLE CLUES (
                ID INT PRIMARY KEY,
                CATEGORY VARCHAR(100) NOT NULL,
                CLUE VARCHAR(1000) NOT NULL,
                ANSWER VARCHAR(100) NOT NULL,
                VALUE INT NOT NULL,
                ROUND TEXT NOT NULL,
                DATE_ADDED TEXT NOT NULL
            ); """

    cursor_obj.execute(table)

    print("Table is Ready")

    # Close the connection
    connection_obj.close()


def load_clues_from_game(game_number):
    print(game_number)
    # JSON to return
    game_JSON = {}
    game_JSON["categories_sj"] = []
    game_JSON["categories_dj"] = []
    game_JSON["categories_fj"] = []
    game_JSON["clues_sj"] = {}
    game_JSON["clues_dj"] = {}
    game_JSON["clues_fj"] = {}

    # J! Archive URL
    url = "http://j-archive.com/showgame.php?game_id=" + str(game_number)

    # Open connection
    u_client = u_req(url)

    # Get source html and parse with soup
    page_html = u_client.read()
    u_client.close()
    page_soup = soup(page_html, "html.parser")

    # Get map of jeopardy round to category list
    categories = page_soup.findAll("td", {"class": "category_name"})
    categories_list = [cat.getText() for cat in categories]

    category_counter = 0
    while category_counter < 13:
        category_counter_str = str(category_counter % 6)
        cateogory_to_append = {}
        cateogory_to_append["title"] = categories_list[category_counter]
        cateogory_to_append["clues"] = [category_counter_str + "-0", category_counter_str + "-1",
            category_counter_str + "-2", category_counter_str + "-3", category_counter_str + "-4"]
        if category_counter < 6:
            game_JSON["categories_sj"].append(cateogory_to_append)
        elif category_counter < 12:
            game_JSON["categories_dj"].append(cateogory_to_append)
        else:
            cateogory_to_append["clues"] = ["0-0"]
            game_JSON["categories_fj"].append(cateogory_to_append)
        category_counter += 1

    # Get clue attrs
    clues = page_soup.findAll("td", {"class": "clue"})

    # Extract text, id, value, and answer from the clue
    clue_questions = [clue.findAll("td", {"class": "clue_text"})[0].getText()
        for clue in clues if clue.div is not None]
    clue_ids = [clue.div.findAll("td", {"class": "clue_unstuck"})[0]['id'] for clue in clues
        if clue.div is not None and len(clue.div.findAll("td", {"class": "clue_unstuck"})) > 0]
    clue_answers = [clue.div['onmouseover'].split("correct_response\">")[1].split("</em>")[0] for clue in clues
        if clue.div is not None]

    clean_clue_answers = []
    for answer in clue_answers:
        clean_answer = answer.replace("<i>", "").replace("</i>", "").replace("\\", "")
        clean_clue_answers.append(clean_answer)

    # Exclude clues that they didn't get to during the game
    all_clue_ids = [
        'clue_J_1_1_stuck',  'clue_J_2_1_stuck',  'clue_J_3_1_stuck',  'clue_J_4_1_stuck',  'clue_J_5_1_stuck',
        'clue_J_6_1_stuck',  'clue_J_1_2_stuck',  'clue_J_2_2_stuck',  'clue_J_3_2_stuck',  'clue_J_4_2_stuck',
        'clue_J_5_2_stuck',  'clue_J_6_2_stuck',  'clue_J_1_3_stuck',  'clue_J_2_3_stuck',  'clue_J_3_3_stuck',
        'clue_J_4_3_stuck',  'clue_J_5_3_stuck',  'clue_J_6_3_stuck',  'clue_J_1_4_stuck',  'clue_J_2_4_stuck',
        'clue_J_3_4_stuck',  'clue_J_4_4_stuck',  'clue_J_5_4_stuck',  'clue_J_6_4_stuck',  'clue_J_1_5_stuck',
        'clue_J_2_5_stuck',  'clue_J_3_5_stuck',  'clue_J_4_5_stuck',  'clue_J_5_5_stuck',  'clue_J_6_5_stuck',
        'clue_DJ_1_1_stuck', 'clue_DJ_2_1_stuck', 'clue_DJ_3_1_stuck', 'clue_DJ_4_1_stuck', 'clue_DJ_5_1_stuck',
        'clue_DJ_6_1_stuck', 'clue_DJ_1_2_stuck', 'clue_DJ_2_2_stuck', 'clue_DJ_3_2_stuck', 'clue_DJ_4_2_stuck',
        'clue_DJ_5_2_stuck', 'clue_DJ_6_2_stuck', 'clue_DJ_1_3_stuck', 'clue_DJ_2_3_stuck', 'clue_DJ_3_3_stuck',
        'clue_DJ_4_3_stuck', 'clue_DJ_5_3_stuck', 'clue_DJ_6_3_stuck', 'clue_DJ_1_4_stuck', 'clue_DJ_2_4_stuck',
        'clue_DJ_3_4_stuck', 'clue_DJ_4_4_stuck', 'clue_DJ_5_4_stuck', 'clue_DJ_6_4_stuck', 'clue_DJ_1_5_stuck',
        'clue_DJ_2_5_stuck', 'clue_DJ_3_5_stuck', 'clue_DJ_4_5_stuck', 'clue_DJ_5_5_stuck', 'clue_DJ_6_5_stuck'
    ]
    excluded_clues = list(set(all_clue_ids).difference(clue_ids))
    for ex_clue in excluded_clues:
        all_clue_ids[all_clue_ids.index(ex_clue)] = "unused"

    # Add clues to JSON
    for i in range(len(clue_ids)):
        row_str = str((i % 6))
        if i < 30:
            col_str = str(math.floor(i / 6))
            add_clue_sj = {}
            if all_clue_ids[i] == "unused":
                add_clue_sj["question"] = "Unused question"
                add_clue_sj["answer"] = "Unused answer"
            else:
                add_clue_sj["question"] = clue_questions[i]
                add_clue_sj["answer"] = clean_clue_answers[i]
            add_clue_sj["value"] = (math.floor(i / 6) + 1) * 200
            add_clue_sj["is_dd"] = False
            game_JSON["clues_sj"][row_str + "-" + col_str] = add_clue_sj
        else:
            col_str = str((math.floor(i / 6) - 5))
            add_clue_dj = {}
            if all_clue_ids[i] == "unused":
                add_clue_dj["question"] = "Unused question"
                add_clue_dj["answer"] = "Unused answer"
            else:
                add_clue_dj["question"] = clue_questions[i]
                add_clue_dj["answer"] = clean_clue_answers[i]
            add_clue_dj["value"] = (math.floor((i - 30) / 6) + 1) * 400
            add_clue_dj["is_dd"] = False
            game_JSON["clues_dj"][row_str + "-" + col_str] = add_clue_dj

    # Handle final jeopardy separately
    final_jeopardy = page_soup.findAll("table", {"class": "final_round"})[0]
    add_clue_fj = {}
    add_clue_fj["question"] = page_soup.findAll("td", {"id": "clue_FJ"})[0].getText()
    add_clue_fj["answer"] = final_jeopardy.div['onmouseover'].split("correct_response")[1].split("</em>")[0][3:]
    add_clue_fj["value"] = 10000
    add_clue_fj["is_dd"] = False
    game_JSON["clues_fj"]["0-0"] = add_clue_fj
    return game_JSON

def get_clue_list_from_json(clues_json, game_id):
    today = date.today()
    f = open("game_id_to_date.json")
    game_id_to_date = json.load(f)

    clues = []
    categories_sj = clues_json["categories_sj"]
    categories_dj = clues_json["categories_dj"]
    categories_fj = clues_json["categories_fj"]

    clues_sj = clues_json["clues_sj"]
    clues_dj = clues_json["clues_dj"]
    clues_fj = clues_json["clues_fj"]

    # Single Jeopardy
    for clue_number, clue_info in clues_sj.items():
        category = ""
        for category in categories_sj:
            clue_number_list = category["clues"]
            if clue_number in clue_number_list:
                category = category["title"]
                break
        sql_id = str(uuid.uuid4())
        clue = [sql_id, category, clue_info["question"], clue_info["answer"], int(clue_info["value"]),
            "Jeopardy", game_id, game_id_to_date[str(game_id)], today.strftime("%Y-%m-%d"), 0, 0]
        clues.append(clue)

    # Double Jeopardy
    for clue_number, clue_info in clues_dj.items():
        category = ""
        for category in categories_dj:
            clue_number_list = category["clues"]
            if clue_number in clue_number_list:
                category = category["title"]
                break
        sql_id = str(uuid.uuid4())
        clue = [sql_id, category, clue_info["question"], clue_info["answer"], int(clue_info["value"]),
            "Double", game_id, game_id_to_date[str(game_id)], today.strftime("%Y-%m-%d"), 0, 0]
        clues.append(clue)

    # Final Jeopardy
    final_category = categories_fj[0]["title"]
    final_clue_info = clues_fj["0-0"]
    final_sql_id = str(uuid.uuid4())
    final_clue = [final_sql_id, final_category, final_clue_info["question"], final_clue_info["answer"], 0,
        "Final", game_id, game_id_to_date[str(game_id)], today.strftime("%Y-%m-%d"), 0, 0]
    clues.append(final_clue)
    clues = replace_quotes(clues)
    return clues

def replace_quotes(clue_list):
    single_quote = "'"
    double_quote = '"'
    clean_clue_list = []
    for clue in clue_list:
        clean_clue = []
        for clue_piece in clue:
            if isinstance(clue_piece, str) == True and (single_quote in clue_piece or double_quote in clue_piece):
                clue_piece = clue_piece.replace('"', '""')
                clue_piece = clue_piece.replace("'", "''")
            clean_clue.append(clue_piece)
        clean_clue_list.append(clean_clue)
    return clean_clue_list

def main():
    # Command line args
    if len(sys.argv) != 3 and len(sys.argv) != 2:
        print("Usage: ./store_clues required: <first game number> optional: <last game number>")
        return

    # Get clues for each game
    cur_game_number = int(sys.argv[1])
    if len(sys.argv) == 3:
        max_game_number = int(sys.argv[2])
    else:
        max_game_number = cur_game_number

    # PostgreSQL
    conn = pgsql.connect(
        database = "joevandevusse",
        user = "joevandevusse",
        password = "whombovb2508",
        host =  "127.0.0.1",
        port = "5432"
    )
    conn.autocommit = True
    cursor = conn.cursor()
    cursor.execute("SELECT * from clues;")

    # Get season 39 games from local file
    s39_game_list = []
    file = open("game_id_to_date.json")
    game_id_to_date = json.load(file)
    for game_id in game_id_to_date.keys():
        if int(game_id) >= cur_game_number and int(game_id) <= max_game_number:
            s39_game_list.append(int(game_id))
    file.close()

    # Get clues from games and insert them into the DB
    for cur_game_number in s39_game_list:
        json_clues = load_clues_from_game(cur_game_number)
        clues = get_clue_list_from_json(json_clues, cur_game_number)

        for clue in clues:
            insert_statement = ""
            insert_statement += "INSERT INTO clues "
            insert_statement += "(category, clue, answer, value, round, game_id, game_date, added_date) "
            insert_statement += "values "
            insert_statement += "('" + clue[1].replace("'", "\'") + "','" + clue[2].replace("'", "\'") + \
                "','" + clue[3].replace("'", "\'") + "'," + str(clue[4]) + ","
            insert_statement += "'" + clue[5] + "'," + str(clue[6]) + ",'" + clue[7] + "','" + clue[8] + "');"
            cursor.execute(insert_statement)

        cur_game_number += 1

    cursor.close()
    conn.close()

if __name__ == "__main__":
    main()
