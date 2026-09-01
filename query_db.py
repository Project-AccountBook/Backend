import mysql.connector

try:
    conn = mysql.connector.connect(
        host="localhost",
        user="jointliving",
        password="jointliving",
        database="jointliving"
    )
    cursor = conn.cursor()
    
    tables = ["group_purchase", "board", "comment", "group_purchase_category", "users"]
    for t in tables:
        print(f"\n--- Table: {t} ---")
        try:
            if t == "users":
                cursor.execute(f"SELECT id, username, email FROM {t} LIMIT 10")
            elif t == "group_purchase_category":
                cursor.execute(f"SELECT id, name FROM {t} LIMIT 10")
            elif t == "comment":
                cursor.execute(f"SELECT id, content FROM {t} LIMIT 10")
            else:
                cursor.execute(f"SELECT id, title FROM {t} LIMIT 10")
            
            rows = cursor.fetchall()
            for row in rows:
                print(row)
        except Exception as e:
            print(f"Error querying {t}: {e}")
            
    conn.close()
except Exception as e:
    print(f"Connection error: {e}")
