package net.sf.sitemonitoring.service;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import net.sf.sitemonitoring.entity.Check;
import net.sf.sitemonitoring.entity.Check.CheckCondition;
import net.sf.sitemonitoring.entity.Check.CheckType;
import net.sf.sitemonitoring.entity.Check.HttpMethod;
import net.sf.sitemonitoring.entity.Check.IntervalType;
import net.sf.sitemonitoring.entity.Configuration;
import net.sf.sitemonitoring.repository.CheckRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InitDbService {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CheckRepository checkRepository;

    @PostConstruct
    public void init() {
        if(configurationService.find() != null) {
            log.info("Configuration detected, won't perform database initialization");
            return;
        }
        log.info("*** DATABASE INIT STARTED ***");
        Configuration configuration = new Configuration();
        configuration.setMonitoringVersion("4.0.0");
        configuration.setEmailSubject("sitemonitoring error");
        configuration.setEmailBody("check name:{CHECK-NAME}\n\ncheck url: {CHECK-URL}\n\nerror:\n{ERROR}");
        configuration.setDefaultSingleCheckInterval(5);
        configuration.setDefaultSitemapCheckInterval(30);
        configuration.setDefaultSpiderCheckInterval(60);
        configuration.setDefaultSendEmails(true);
        configuration.setSocketTimeout(20000);
        configuration.setConnectionTimeout(20000);
        configuration.setTooLongRunningCheckMinutes(30);
        configuration.setCheckBrokenLinks(false);
        configuration.setAdminUsername("admin");
        configuration.setAdminPassword(new BCryptPasswordEncoder().encode("admin"));
        configuration.setSendEmails(false);
        configuration.setUserAgent("sitemonitoring http://sitemonitoring.sourceforge.net");
        configuration.setInfoMessage("Please don't monitor my websites (like javavids.com and sitemonitoring.sourceforge.net). Lot's of people started doing it and effectively DDOSed them. If you monitor them anyway, your IP address will be blocked!");

        configurationService.save(configuration);

        Check check = new Check();
        check.setName("check example homepage");
        check.setUrl("http://www.example.com/");
        check.setConditionType(CheckCondition.CONTAINS);
        check.setCondition("</html>");
        check.setType(CheckType.SINGLE_PAGE);
        check.setCheckBrokenLinks(false);
        check.setSendEmails(true);
        check.setSocketTimeout(20000);
        check.setConnectionTimeout(20000);
        check.setScheduledInterval(1);
        check.setChartPeriodType(IntervalType.HOUR);
        check.setChartPeriodValue(1);
        check.setHttpMethod(HttpMethod.GET);
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        check.setScheduledStartDate(calendar.getTime());
        check.setScheduledIntervalType(IntervalType.MINUTE);
        checkRepository.save(check);

        log.info("*** DATABASE INIT FINISHED ***");
    }

}
