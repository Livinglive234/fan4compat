package dev.fan4.compat.updater;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/** Only versioned, Minecraft-1.21.1 release assets from the project's public repo. */
public final class ReleaseUpdates {
    public static final String REPO="Livinglive234/fan4compat";
    private static final Pattern VERSION=Pattern.compile("v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-(alpha|beta|rc)\\.(\\d+))?");
    private static final Pattern SHA=Pattern.compile("[0-9a-fA-F]{64}");
    public record Release(String version,URI jar,URI checksum,String digest) {}
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    public static int compare(String left,String right) {
        Matcher a=VERSION.matcher(left),b=VERSION.matcher(right);
        if(!a.matches()||!b.matches())throw new IllegalArgumentException("Unsupported release version");
        for(int i=1;i<=3;i++){int c=Long.compare(Long.parseLong(a.group(i)),Long.parseLong(b.group(i)));if(c!=0)return c;}
        int c=Integer.compare(rank(a.group(4)),rank(b.group(4)));if(c!=0)return c;
        return Long.compare(a.group(5)==null?0:Long.parseLong(a.group(5)),b.group(5)==null?0:Long.parseLong(b.group(5)));
    }
    private static int rank(String value){return value==null?3:switch(value){case "alpha"->0;case "beta"->1;default->2;};}
    private static URI asset(String url) {
        URI uri=URI.create(url);
        if(!"https".equals(uri.getScheme())||!"github.com".equals(uri.getHost())||uri.getRawUserInfo()!=null||uri.getPort()!=-1||!uri.getPath().startsWith("/"+REPO+"/releases/download/"))throw new IllegalArgumentException("Unexpected update asset URL");
        return uri;
    }
    public static Release select(String json,String installed,boolean prereleases) {
        Release newest=null;
        for(JsonElement value:JsonParser.parseString(json).getAsJsonArray()) {
            JsonObject release=value.getAsJsonObject();
            if(release.get("draft").getAsBoolean()||(!prereleases&&release.get("prerelease").getAsBoolean()))continue;
            String tag=release.get("tag_name").getAsString();if(!VERSION.matcher(tag).matches())continue;
            String version=tag.startsWith("v")?tag.substring(1):tag;
            if(compare(version,installed)<=0||newest!=null&&compare(version,newest.version())<=0)continue;
            String filename="Fan4Compat-"+version+".jar",digest=null;URI jar=null,checksum=null;
            for(JsonElement item:release.getAsJsonArray("assets")) {
                JsonObject a=item.getAsJsonObject();String name=a.get("name").getAsString();
                if(name.equals(filename)){jar=asset(a.get("browser_download_url").getAsString());JsonElement d=a.get("digest");if(d!=null&&!d.isJsonNull()&&d.getAsString().startsWith("sha256:"))digest=d.getAsString().substring(7);}
                if(name.equals(filename+".sha256"))checksum=asset(a.get("browser_download_url").getAsString());
            }
            if(jar!=null&&(digest!=null&&SHA.matcher(digest).matches()||checksum!=null))newest=new Release(version,jar,checksum,digest);
        }
        return newest;
    }
    private InputStream request(URI uri)throws IOException,InterruptedException {
        HttpRequest request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).header("User-Agent","Fan4Compat-Updater").GET().build();
        HttpResponse<InputStream> response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());
        if(response.statusCode()!=200){response.body().close();throw new IOException("GitHub update HTTP "+response.statusCode());}
        return response.body();
    }
    private String text(URI uri,int limit)throws IOException,InterruptedException {
        try(InputStream in=request(uri)){byte[] bytes=in.readNBytes(limit+1);if(bytes.length>limit)throw new IOException("Update response too large");return new String(bytes,java.nio.charset.StandardCharsets.UTF_8);}
    }
    public Release check(String current,boolean prereleases)throws IOException,InterruptedException {
        return select(text(URI.create("https://api.github.com/repos/"+REPO+"/releases?per_page=30"),2*1024*1024),current,prereleases);
    }
    public Path download(Release release,Path directory)throws IOException,InterruptedException {
        Files.createDirectories(directory);Path part=Files.createTempFile(directory,"download-",".part");
        try {
            String digest=release.digest();
            if(digest==null||!SHA.matcher(digest).matches()){
                String checksum=text(release.checksum(),1024).trim();digest=checksum.split("\\s+",2)[0];
                if(!SHA.matcher(digest).matches())throw new IOException("Invalid update checksum");
            }
            try(InputStream in=request(release.jar());OutputStream out=Files.newOutputStream(part)) {
                byte[] buffer=new byte[8192];long length=0;int n;
                while((n=in.read(buffer))!=-1){length+=n;if(length>32*1024*1024)throw new IOException("Update jar too large");out.write(buffer,0,n);}
            }
            validate(part,release.version(),digest);
            return part;
        }catch(IOException|InterruptedException|RuntimeException failure){Files.deleteIfExists(part);throw failure;}
    }
    public static void validate(Path jar,String version,String digest)throws IOException {
        if(!UpdateInstaller.digest(jar).equalsIgnoreCase(digest))throw new IOException("Downloaded update checksum mismatch");
        try(ZipFile zip=new ZipFile(jar.toFile())) {
            ZipEntry metadata=zip.getEntry("fabric.mod.json");if(metadata==null)throw new IOException("Missing mod metadata");
            try(InputStream in=zip.getInputStream(metadata)) {
                byte[] bytes=in.readNBytes(65537);if(bytes.length>65536)throw new IOException("Mod metadata too large");
                JsonObject mod=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                if(!mod.get("id").getAsString().equals("fan4compat")||!mod.get("version").getAsString().equals(version)||!mod.getAsJsonObject("depends").get("minecraft").getAsString().equals("1.21.1"))throw new IOException("Update mod/version/Minecraft mismatch");
            }
        }catch(JsonParseException|IllegalStateException|NullPointerException bad){throw new IOException("Invalid update metadata",bad);}
    }
}
