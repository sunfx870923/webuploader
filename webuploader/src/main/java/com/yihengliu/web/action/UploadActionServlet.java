package com.yihengliu.web.action;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 切片合并上传文件
 */
public class UploadActionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private String serverPath = "c:\\temp";

    private Log log = LogFactory.getLog(getClass());

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String action = request.getParameter("action");

        if ("mergeChunks".equals(action)) {
            //合并文件操作
            this.mergeChunks(request);
        } else if ("checkChunk".equals(action)) {
            //检查文件操作
            this.checkChunk(request, response);
        }
    }

    /**
     * checkChunk 检查
     * 
     * @Description
     * @author FeiXiangSun
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void checkChunk(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 校验文件是否已经上传并返回结果给前端
        // 文件唯一表示
        String fileMd5 = request.getParameter("fileMd5");
        // 当前分块下标
        String chunk = request.getParameter("chunk");
        // 当前分块大小
        String chunkSize = request.getParameter("chunkSize");
        // 找到分块文件
        File checkFile = new File(serverPath + "/" + fileMd5 + "/" + chunk);
        // 检查文件是否存在，且大小一致
        response.setContentType("text/html;charset=utf-8");
        if (checkFile.exists() && checkFile.length() == Integer.parseInt((chunkSize))) {
            response.getWriter().write("{\"ifExist\":1}");
        } else {
            response.getWriter().write("{\"ifExist\":0}");
        }
    }

    /**
     * 合并文件
     * 
     * @Description
     * @author FeiXiangSun
     * @param request
     * @throws ServletException
     * @throws IOException
     */
    @SuppressWarnings("unused")
    public void mergeChunks(HttpServletRequest request) throws ServletException, IOException {
        
        // 带后缀文件完整名
        String fileName = request.getParameter("fileName");
        // 获得需要合并的目录
        String fileMd5 = request.getParameter("fileMd5");
        // 读取目录所有文件
        File f = new File(serverPath + "/" + fileMd5);
        File[] fileArray = f.listFiles(new FileFilter() {
            // 排除目录，只要文件
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return false;
                }
                return true;
            }
        });

        // 转成集合，便于排序
        List<File> fileList = new ArrayList<File>(Arrays.asList(fileArray));
        // 从小到大排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (Integer.parseInt(o1.getName()) < Integer.parseInt(o2.getName())) {
                    return -1;
                }
                return 1;
            }
        });

        // 新建保存文件
        File outputFile = new File(serverPath + "/" + UUID.randomUUID().toString() + ".zip");

        // 创建文件
        outputFile.createNewFile();

        // 输出流
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        FileChannel outChannel = fileOutputStream.getChannel();

        // 合并
        FileChannel inChannel;
        for (File file : fileList) {
            inChannel = new FileInputStream(file).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inChannel.close();
            // 删除分片
            file.delete();
        }

        // 关闭流
        fileOutputStream.close();
        outChannel.close();

        // 清除文件加
        File tempFile = new File(serverPath + "/" + fileMd5);
        if (tempFile.isDirectory() && tempFile.exists()) {
            tempFile.delete();
        }

        log.debug("合并文件成功");


    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
