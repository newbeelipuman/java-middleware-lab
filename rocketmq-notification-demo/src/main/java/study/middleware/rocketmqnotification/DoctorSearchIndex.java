package study.middleware.rocketmqnotification;

import java.util.List;

public interface DoctorSearchIndex {

    int rebuild(List<Doctor> doctors);

    SearchPage<Doctor> search(String keyword, int page, int size);
}
