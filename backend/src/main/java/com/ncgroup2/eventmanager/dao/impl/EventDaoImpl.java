package com.ncgroup2.eventmanager.dao.impl;

import com.ncgroup2.eventmanager.dao.EventDao;
import com.ncgroup2.eventmanager.entity.Event;
import com.ncgroup2.eventmanager.util.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public class EventDaoImpl extends JdbcDaoSupport implements EventDao {

    @Autowired
    private QueryService queryService;

    @Autowired
    DataSource dataSource;

    @PostConstruct
    private void initialize() {
        setDataSource(dataSource);
    }

    @Override
    public void createEvent(Event event, int visibility, int eventStatus) {

        UUID groupId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        String query_event = "INSERT INTO \"Event\" " +
                "(id,name,group_id,folder_id,creator_id,start_time,end_time,visibility,description,status) " +
                "VALUES(?,?, ?, NULL, CAST(? AS UUID),?,?,?,?,?)";

        Object[] eventParams = new Object[]{
                eventId,
                event.getName(),
                groupId,
                event.getCreatorId(),
                event.getStartTime(),
                event.getEndTime(),
                visibility,
                event.getDescription(),
                eventStatus
        };
        this.getJdbcTemplate().update(query_event, eventParams);

        String query_customer_event = "INSERT INTO \"Customer_Event\"" +
                "(id,event_id,customer_id,start_date_notification,frequency_value,priority,status)" +
                "VALUES(uuid_generate_v1(),?,CAST(? AS UUID),?,1,(SELECT id FROM \"Customer_Event_Priority\" WHERE name = 'LOW')," +
                "(SELECT id FROM \"Customer_Event_Status\" WHERE name = 'ACCEPTED'))";

        Object[] customerEventParams = new Object[]{
                eventId,
                event.getCreatorId(),
                event.getStartTime()
        };
        this.getJdbcTemplate().update(query_customer_event, customerEventParams);
    }


    @Override
    public void deleteEvent(Event event) {
        String query = "DELETE FROM Event WHERE id = CAST (? AS UUID)";
        Object[] params = new Object[]{
                event.getId()
        };
        this.getJdbcTemplate().update(query, params);
    }

    @Override
    public void deleteEvent(String eventId) {
        String sql = "UPDATE \"Event\" SET status = (SELECT id FROM \"Event_Status\" WHERE name = 'DELETED') " +
                     "WHERE id = CAST (? AS uuid)";
        Object[] params = new Object[]{
                eventId
        };
        this.getJdbcTemplate().update(sql, params);
    }

    @Override
    public void updateField(Event event, String fieldName, Object fieldValue) {
        String sql = "UPDATE \"Event\" SET " + fieldName + " = ? WHERE id = CAST (? AS uuid)";
        Object[] params = new Object[]{
                fieldValue,
                event.getId()
        };
        this.getJdbcTemplate().update(sql, params);

    }

    @Override
    public int getStatusId(String fieldValue) {
        String sql = "SELECT id FROM \"Event_Status\" WHERE name = ?";
        Object[] params = new Object[]{
                fieldValue
        };
        return this.getJdbcTemplate().queryForObject(sql, params, int.class);
    }

    @Override
    public int getVisibilityId(String fieldValue) {
        String sql = "SELECT id FROM \"Event_Visibility\" WHERE name = ?";
        Object[] params = new Object[]{
                fieldValue
        };
        return this.getJdbcTemplate().queryForObject(sql, params, int.class);
    }

    @Override
    public List<Event> getEventsByCustId(String custId) {
        String sql = "SELECT \"Event\".id AS id, \"Event\".name AS name, start_time, end_time, " +
                "\"Event\".description AS description," +
                "\"Event_Visibility\".name AS visibility " +
                "FROM (\"Event\" INNER JOIN \"Event_Visibility\" " +
                "ON \"Event\".visibility = \"Event_Visibility\".id) " +
                "WHERE creator_id = CAST(? AS UUID) " +
                "AND start_time IS NOT NULL " +
                "AND end_time IS NOT NULL";// +
//                " AND \"Event_Status\".name <> 'DELETED'";
        Object[] params = new Object[]{
                custId
        };
        return this.getJdbcTemplate().query(sql, params, new BeanPropertyRowMapper(Event.class));
    }

    @Override
    public Event getById(String id) {
        String sql = "SELECT \"Event\".id AS id, \"Event\".creator_id AS creator_id, \"Event\".name AS name, " +
                "start_time, end_time, \"Event\".description AS description," +
                "\"Event_Visibility\".name AS visibility " +
                "FROM (\"Event\" INNER JOIN \"Event_Visibility\" " +
                "ON \"Event\".visibility = \"Event_Visibility\".id) " +
                "WHERE \"Event\".id  = CAST(? AS UUID) " +
                "AND start_time IS NOT NULL " +
                "AND end_time IS NOT NULL";

        Object[] params = new Object[]{
                id
        };

//        EventMapper mapper = new EventMapper();

        List<Event> list = this.getJdbcTemplate().query(sql, params, new BeanPropertyRowMapper(Event.class));
        if (!list.isEmpty()) {
            return list.iterator().next();
        } else {
            return null;
        }
    }

    @Override
    public List<Event> getAllPublicAndFriends(String customerId) {
        String sql = "SELECT\n" +
                "  \"Event\".id              AS id,\n"+
                "  \"Event\".name            AS name,\n" +
                "  start_time,\n" +
                "  end_time,\n" +
                "  \"Event\".description     AS description,\n" +
                "  \"Event_Visibility\".name AS visibility\n" +
                "FROM (\"Event\"\n" +
                "  INNER JOIN \"Event_Visibility\"\n" +
                "    ON \"Event\".visibility = \"Event_Visibility\".id)\n" +
                "WHERE \"Event\".visibility = (SELECT id\n" +
                "                            FROM \"Event_Visibility\"\n" +
                "                            WHERE name = 'PUBLIC')\n" +
                "      AND start_time IS NOT NULL\n" +
                "      AND end_time IS NOT NULL\n" +
                "\n" +
                "UNION\n" +
                "\n" +
                "SELECT\n" +
                "  \"Event\".id              AS id,\n"+
                "  \"Event\".name            AS name,\n" +
                "  start_time,\n" +
                "  end_time,\n" +
                "  \"Event\".description     AS description,\n" +
                "  \"Event_Visibility\".name AS visibility\n" +
                "FROM (\"Event\"\n" +
                "  INNER JOIN \"Event_Visibility\"\n" +
                "    ON \"Event\".visibility = \"Event_Visibility\".id)\n" +
                "WHERE \"Event\".visibility = (SELECT id\n" +
                "                            FROM \"Event_Visibility\"\n" +
                "                            WHERE name = 'FRIENDS')\n" +
                "\n" +
                "       AND (\"isFriends\"(creator_id, cast(? as uuid))\n" +
                "           OR creator_id = cast(? as uuid))\n" +
                "\n" +
                "\n" +
                "      AND start_time IS NOT NULL\n" +
                "      AND end_time IS NOT NULL;\n";

        Object[] params = new Object[]{
                customerId,
                customerId
        };

        return this.getJdbcTemplate().query(sql, params, new BeanPropertyRowMapper(Event.class));
    }

    @Override
    public boolean isParticipant(String customerId, String eventId) {
        String sql ="SELECT * FROM \"Customer_Event\" where event_id = cast (? as uuid) and customer_id = cast(? as uuid)";
        Object[] params = new Object[]{
                customerId,
                eventId
        };

        return !this.getJdbcTemplate().query(sql, params, (resultSet, i) -> resultSet.next()).isEmpty();
    }

    @Override
    public void removeParticipant(String customerId, String eventId) {
        String sql = "DELETE FROM \"Customer_Event\" WHERE customer_id = cast(? as uuid) and event_id = cast(? as uuid)";
        Object[] params = new Object[] {
                customerId,
                eventId
        };

        this.getJdbcTemplate().update(sql,params);
    }

    @Override
    public void addParticipant(String customerId, String eventId) {
        String sql = "INSERT INTO \"Customer_Event\" (customer_id, event_id, status, priority) VALUES (cast(? as uuid), cast(? as uuid), (SELECT id FROM \"Customer_Event_Status\" WHERE name = 'ACCEPTED')," +
                "(SELECT id FROM \"Customer_Event_Priority\" WHERE name = 'AVERAGE'))";
        Object[] params = new Object[]{
                customerId,
                eventId
        };

        this.getJdbcTemplate().update(sql,params);
    }
}
