package org.web.service;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.spatial.tier.DistanceFieldComparatorSource;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LuceneSpatial {

    private Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_32);;
    private IndexWriter writer;
    private FSDirectory indexDirectory;
    private IndexSearcher indexSearcher;
    private IndexReader indexReader;
    private String indexPath = "src/main/resources/static/lucene-spatial-all";

    // Spatial
    private IProjector projector;
    private CartesianTierPlotter ctp;
//    public static final double RATE_MILE_TO_KM = 1.609344; //英里和公里的比率
    public static final double RATE_MILE_TO_KM = 1;
    public static final String LAT_FIELD = "lat";
    public static final String LON_FIELD = "lng";
    private static final double MAX_RANGE = 15.0; // 索引支持的最大范围，单位是千米
    private static final double MIN_RANGE = 3.0;  // 索引支持的最小范围，单位是千米
    private int startTier;
    private int endTier;

    public LuceneSpatial() {
    }

    public void init() throws Exception {
        initializeSpatialOptions();

//        analyzer = new StandardAnalyzer(Version.LUCENE_32);

        File path = new File(indexPath);

        boolean isNeedCreateIndex = true;

        if (path.exists() && !path.isDirectory())
            throw new Exception("Specified path is not a directory");

        if (!path.exists()) {
            path.mkdirs();
            isNeedCreateIndex = true;
        }

        indexDirectory = FSDirectory.open(new File(indexPath));

        //建立索引
        if (isNeedCreateIndex) {
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(
                    Version.LUCENE_32, analyzer);
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            writer = new IndexWriter(indexDirectory, indexWriterConfig);
            buildIndex();
        }

        indexReader = IndexReader.open(indexDirectory, true);
        indexSearcher = new IndexSearcher(indexReader);

    }

    @SuppressWarnings("deprecation")
    private void initializeSpatialOptions() {
        projector = new SinusoidalProjector();
        ctp = new CartesianTierPlotter(0, projector,
                CartesianTierPlotter.DEFALT_FIELD_PREFIX);
        startTier = ctp.bestFit(MAX_RANGE / RATE_MILE_TO_KM);
        endTier = ctp.bestFit(MIN_RANGE / RATE_MILE_TO_KM);
    }


    private int mile2Meter(double miles) {
        double dMeter = miles * RATE_MILE_TO_KM * 1000;

        return (int) dMeter;
    }

    private double km2Mile(double km) {
        return km / RATE_MILE_TO_KM;
    }

    private void buildIndex() {
        BufferedReader br = null;
        try {
            //逐行添加测试数据到索引中，测试数据文件和源文件在同一个目录下
//            br = new BufferedReader(new InputStreamReader(
//                    LuceneSpatial.class.getResourceAsStream("/all.json")));
            br =new BufferedReader(
                    new FileReader("/Users/apple/Documents/UCR-semester3/172/project_data/pythoncode/all.json"));
            String line = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if(line.startsWith("["))    line = line.substring(1);
                if(line.endsWith("]"))  line = line.substring(0, line.length() - 1);
                index(new JSONObject(line));
            }

            writer.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void index(JSONObject object) throws Exception {
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

        //将位置信息添加到索引中
        indexLocation(document, object);

        writer.addDocument(document);
    }

    private void indexLocation(Document document, JSONObject object)
            throws Exception {

        String coordinate = String.valueOf(object.get("coordinate"));
        //[[[-74.041878, 40.570842], [-74.041878, 40.739434], [-73.855673, 40.739434], [-73.855673, 40.570842]]]
        String s[] = coordinate.split(",");
        double longitude = 0.0;
        double latitude = 0.0;
        if(s.length == 8){
            int i = 0;
            for (String t : s) {
                t = t.replace("[", "").replace("]", "");
                t = t.trim();
                double d = Double.valueOf(t);
//                System.out.println(d);
                if (i % 2 == 0) longitude += d;
                else latitude += d;
                i++;
            }
        }
        longitude /= 4;
        latitude /= 4;
        System.out.println(longitude + " , " + latitude);
        document.add(new Field("lat", NumericUtils
                .doubleToPrefixCoded(latitude), Field.Store.YES,
                Field.Index.NOT_ANALYZED));
        document.add(new Field("lng", NumericUtils
                .doubleToPrefixCoded(longitude), Field.Store.YES,
                Field.Index.NOT_ANALYZED));
        for (int tier = startTier; tier <= endTier; tier++) {
            ctp = new CartesianTierPlotter(tier, projector,
                    CartesianTierPlotter.DEFALT_FIELD_PREFIX);
            final double boxId = ctp.getTierBoxId(latitude, longitude);
            document.add(new Field(ctp.getTierFieldName(), NumericUtils
                    .doubleToPrefixCoded(boxId), Field.Store.YES,
                    Field.Index.NOT_ANALYZED_NO_NORMS));
        }
    }

    public List<lucenetest> search(String keyword, double longitude,
                               double latitude, double range) throws Exception {
        initializeSpatialOptions();
        List<lucenetest> result = new ArrayList<>();
        double miles = km2Mile(range);
        DistanceQueryBuilder dq = new DistanceQueryBuilder(latitude,
                longitude, miles, "lat", "lng",
                CartesianTierPlotter.DEFALT_FIELD_PREFIX, true, startTier,
                endTier);
        DistanceFieldComparatorSource dsort = new DistanceFieldComparatorSource(
                dq.getDistanceFilter());
        Sort sort = new Sort(new SortField("geo_distance", dsort));
        Query query = buildQuery(keyword);
        indexDirectory = FSDirectory.open(new File(indexPath));
        indexReader = IndexReader.open(indexDirectory, true);
        indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setDefaultFieldSortScoring(true, false);
        TopDocs hits = indexSearcher.search(query, dq.getFilter(),
                Integer.MAX_VALUE, sort);
        Map<Integer, Double> distances = dq.getDistanceFilter()
                .getDistances();
        System.out.println("total: " + hits.totalHits);
        for (int i = 0; i < hits.totalHits; i++) {
            final int docID = hits.scoreDocs[i].doc;
            final Document doc = indexSearcher.doc(docID);
            final StringBuilder builder = new StringBuilder();
            String created_at = doc.get("created_at");
            if(created_at.indexOf(" ") == -1) {
                long dateTime = Long.valueOf(created_at);
                Date date = new Date(dateTime);
                created_at = date.toString();
            }
            String id = doc.get("id");
            String source = doc.get("source");
            String text = doc.get("text");
            String lang = doc.get("lang");
            String favorite_count = doc.get("favorite_count");
            String retweet_count = doc.get("retweet_count");
            String user_mentions = doc.get("user_mentions");
            String location = doc.get("location");
            String link = doc.get("link");
            String title = doc.get("title");
            double la = NumericUtils.prefixCodedToDouble(doc.get("lat"));
            double ln = NumericUtils.prefixCodedToDouble(doc.get("lng"));
            System.out.println(la);
            System.out.println(ln);
            System.out.println("------------");

            lucenetest cur = new lucenetest(created_at, id, source, text, lang, favorite_count, retweet_count,
                    user_mentions, location, link, title, String.valueOf(la), String.valueOf(ln), String.valueOf(hits.scoreDocs[i].score));

            result.add(cur);

            builder.append("找到了: ")
                    .append("created_at: ").append(created_at)
                    .append("\nid: ").append(id)
                    .append("\nsource: ").append(source)
                    .append("\ntext: ").append(text)
                    .append("\nlang: ").append(lang)
                    .append("\nfavorite_count: ").append(favorite_count)
                    .append("\nretweet_count: ").append(retweet_count)
                    .append("\nuser_mentions: ").append(user_mentions)
                    .append("\nlocation: ").append(location)
                    .append("\nlink: ").append(link)
                    .append("\ntitle: ").append(title)
                    .append("\n距离: ")
                    .append(mile2Meter(distances.get(docID)))
                    .append("迈")
                    .append("\n-------------------------------------");
            System.out.println(builder.toString());

        }
        return result;
    }

    private Query buildQuery(String keyword) throws Exception {
        //如果没有指定关键字，则返回范围内的所有结果
        if (keyword == null || keyword.isEmpty()) {
            return new MatchAllDocsQuery();
        }
        analyzer = new StandardAnalyzer(Version.LUCENE_32);
        QueryParser parser = new QueryParser(Version.LUCENE_32, "text",
                analyzer);

        parser.setDefaultOperator(QueryParser.Operator.AND);

        return parser.parse(keyword.toString());
    }

    public List<lucenetest> searchBasic(String keyword) throws Exception {
        initializeSpatialOptions();
        List<lucenetest> result = new ArrayList<>();
        Query query = buildQuery(keyword);
        indexDirectory = FSDirectory.open(new File(indexPath));
        indexReader = IndexReader.open(indexDirectory, true);
        indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setDefaultFieldSortScoring(true, false);
        Sort sort = new Sort(new SortField("text", SortField.SCORE), new SortField("created_at", SortField.STRING, true));
        TopDocs hits = indexSearcher.search(query, 10000, sort);
        System.out.println("总数量" + hits.scoreDocs.length);
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            System.out.println(i);
            final int docID = hits.scoreDocs[i].doc;
            final Document doc = indexSearcher.doc(docID);
            final StringBuilder builder = new StringBuilder();
            System.out.println(hits.scoreDocs[i].score);
            String created_at = doc.get("created_at");
            if(created_at.indexOf(" ") == -1) {
                long dateTime = Long.valueOf(created_at);
                Date date = new Date(dateTime);
                created_at = date.toString();
            }
            String id = doc.get("id");
            String source = doc.get("source");
            String text = doc.get("text");
            String lang = doc.get("lang");
            String favorite_count = doc.get("favorite_count");
            String retweet_count = doc.get("retweet_count");
            String user_mentions = doc.get("user_mentions");
            String location = doc.get("location");
            String link = doc.get("link");
            String title = doc.get("title");
            String lat = doc.get("lat");
            String lng = doc.get("lng");

            lucenetest cur = new lucenetest(created_at, id, source, text, lang, favorite_count, retweet_count,
                    user_mentions, location, link, title, lat, lng, String.valueOf(hits.scoreDocs[i].score));

            result.add(cur);

//            builder.append("找到了: ")
//                    .append("created_at: ").append(created_at)
//                    .append("\nid: ").append(id)
//                    .append("\nsource: ").append(source)
//                    .append("\ntext: ").append(text)
//                    .append("\nlang: ").append(lang)
//                    .append("\nfavorite_count: ").append(favorite_count)
//                    .append("\nretweet_count: ").append(retweet_count)
//                    .append("\nuser_mentions: ").append(user_mentions)
//                    .append("\nlocation: ").append(location)
//                    .append("\nlink: ").append(link)
//                    .append("\ntitle: ").append(title)
//                    .append("\n-------------------------------------");
//            System.out.println(builder.toString());

            System.out.println("总数量" + hits.scoreDocs.length);
        }
        return result;
    }
}