const { Client } = require("pg");

async function main() {
    const client = new Client({
        ssl: { rejectUnauthorized: false }
    });
    await client.connect();
    const result = await client.query(
        `select table_name, column_name, is_nullable
         from information_schema.columns
         where table_schema = 'public'
           and (
               (table_name = 'quizzes' and column_name = 'quiz_set_id')
               or (table_name = 'quiz_sets' and column_name = 'difficulty')
           )
         order by table_name, column_name`
    );
    console.log(JSON.stringify(result.rows));
    await client.end();
}

main().catch(error => {
    console.error(error.message);
    process.exit(1);
});
