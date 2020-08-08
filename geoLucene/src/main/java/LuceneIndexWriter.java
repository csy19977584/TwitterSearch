import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.util.List;

public class LuceneIndexWriter{
    String indexPath = "";

    String jsonFilePath = "";

    IndexWriter indexWriter = null;

    public LuceneIndexWriter(String indexPath, String jsonFilePath) {
        this.indexPath = indexPath;
        this.jsonFilePath = jsonFilePath;
    }

    public void createIndex(){
        JSONArray jsonObjects = parseJSONFile();
        openIndex();
        addDocuments(jsonObjects);
        finish();
    }

    /**
     * Parse a Json file. The file path should be included in the constructor
     */
    public JSONArray parseJSONFile(){

        InputStream jsonFile =  getClass().getResourceAsStream(jsonFilePath);
        Reader readerJson = new InputStreamReader(jsonFile);

        //Parse the json file using simple-json library
        Object fileObjects= JSONValue.parse(readerJson);
        JSONArray arrayObjects=(JSONArray)fileObjects;

        return arrayObjects;

    }

    public boolean openIndex(){
        try {
            Directory dir = FSDirectory.open(new File(indexPath));
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_32);
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_32, analyzer);

            //Always overwrite the directory
//            iwc.setOpenMode(OpenMode.CREATE);
            indexWriter = new IndexWriter(dir, iwc);

            return true;
        } catch (Exception e) {
            System.err.println("Error opening the index. " + e.getMessage());

        }
        return false;

    }

    /**
     * Add documents to the index
     */
    public void addDocuments(JSONArray jsonObjects){
        for(JSONObject object : (List<JSONObject>) jsonObjects){
            Document document = new Document();
            document.add(new Field("created_at",String.valueOf(object.get("created_at")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("id",String.valueOf(object.get("id")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("source",String.valueOf(object.get("source")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("text",String.valueOf(object.get("text")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("lang",String.valueOf(object.get("lang")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("favorite_count",String.valueOf(object.get("favorite_count")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("retweet_count",String.valueOf(object.get("retweet_count")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("user_mentions",String.valueOf(object.get("user_mentions")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("location",String.valueOf(object.get("location")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("link",String.valueOf(object.get("link")), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field("title",String.valueOf(object.get("title")),Field.Store.YES, Field.Index.ANALYZED));
            try {
                indexWriter.addDocument(document);
            } catch (IOException ex) {
                System.err.println("Error adding documents to the index. " +  ex.getMessage());
            }
        }
    }

    /**
     * Write the document to the index and close it
     */
    public void finish(){
        try {
            indexWriter.commit();
            indexWriter.close();
        } catch (IOException ex) {
            System.err.println("We had a problem closing the index: " + ex.getMessage());
        }
    }
}
