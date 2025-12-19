package team.omok.omok_mini_project.util;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;
import java.sql.Connection;

//@WebListener 주석처리 일단은 Connection으로하기로함
//DbInitListener는 JNDI 방식 전용이라 DriverManager 방식에서는 실행되면 안 됩니다.
public class DbInitListener implements ServletContextListener { //이것만 추가로 올리고

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // 1) JNDI 컨텍스트 얻기
            Context ctx = new InitialContext();

            // 2) context.xml에 등록한 Resource name으로 DataSource 가져오기

            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/omokdb");

            // 3) 전역 저장소(ServletContext)에 저장 -> 모든 Servlet에서 꺼내씀
            ServletContext app = sce.getServletContext();
            app.setAttribute("dataSource", ds);

            System.out.println("[JNDI] DataSource ready: jdbc/omokdb");

            // 4)  실제로 DB 연결 되는지 테스트 로그
            try (Connection con = ds.getConnection()) {
                System.out.println("[DB] connected OK: " + con.getMetaData().getURL());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("JNDI DataSource init failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[JNDI] contextDestroyed");
    }
}

