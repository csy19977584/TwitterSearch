package org.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.web.service.LuceneSpatial;
import org.web.service.lucenetest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Controller
public class Rescontroller {
    List<lucenetest> list = new ArrayList<>();
    String key = "";
    String preindex = "";

    @RequestMapping(value = "/")
    public String success(){
        return "index";
    }

    public List<lucenetest> produceData(String keyword, String index, String lang, String lat) throws Exception {
        System.out.println(keyword);
        System.out.println(index);
        System.out.println("longtitude: " + lang);
        System.out.println("lantitude: " + lat);
        List<lucenetest> res = new ArrayList<>();
        if(index.equals("Lueune")) {
            res = new LuceneSpatial().searchBasic(keyword);
        } else if(index.equals("location")) {
            System.out.println("test1");
            if(!lang.equals("notfound") & !lat.equals("notfound")){
                System.out.println("test2");
                res = new LuceneSpatial().search(keyword, Double.valueOf(lang), Double.valueOf(lat), 10);
            }
        }
        return res;
    }



    @RequestMapping(value = "/context",method = RequestMethod.GET)
    public String search(@RequestParam(value = "search_text") String context, @RequestParam("search_button") String show,
                         @RequestParam("longtitude") String longtitude, @RequestParam("latitude") String latitude,
                         @RequestParam(value = "curPage", defaultValue = "1") String cur, Model model) throws Exception {
        int curPage = Integer.valueOf(cur);
        if(!key.equals(context) || !preindex.equals(show)) {
            key = context;
            preindex = show;
            list = produceData(context, show, longtitude, latitude);
        }
        int totalpage = list.size() / 12 + (list.size() % 12 == 0 ? 0 : 1);
        List<lucenetest> curpagecontent = getContent(curPage);
        model.addAttribute("list", curpagecontent);
        model.addAttribute("curpage", cur);
        model.addAttribute("keyword", context);
        model.addAttribute("index", show);
        model.addAttribute("totalpage", totalpage);
        model.addAttribute("lat",  latitude);
        model.addAttribute("long", longtitude);
        model.addAttribute("spanshow", "Result of " + context +" , index is "+ show +",the current page is "+cur+" / " + totalpage);
        if(show.equals("location")){
            if(longtitude.equals("notfound"))  return "notfound";
            return "stock1";
        }
        return "stock";
    }

    public List<lucenetest> getContent(int startpage){
        if(startpage == 1){
            return list.subList(0, Math.min(12, list.size()));
        }
        int start = (startpage - 1) * 12;
        int end = Math.min(start + 12, list.size());
        return list.subList(start, end);
    }

    @RequestMapping(value = "/map")
    public String details(@RequestParam("userlat") String userlat, @RequestParam("userlng") String userlng,
                          @RequestParam("lat") String lat, @RequestParam("lng") String lng, Model model){
        model.addAttribute("userlat", userlat);
        model.addAttribute("userlng", userlng);
        model.addAttribute("lat", lat);
        model.addAttribute("lng", lng);
        return "map";
    }

    public static String readTxt(String txtPath) {
        File file = new File(txtPath);
        System.err.println("exist: " + file.exists());
        if(file.isFile() && file.exists()){
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuffer sb = new StringBuffer();
                String text = null;
                while((text = bufferedReader.readLine()) != null){
                    sb.append(text);
                }
                return sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
