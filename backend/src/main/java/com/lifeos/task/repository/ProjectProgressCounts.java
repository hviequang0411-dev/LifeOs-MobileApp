package com.lifeos.task.repository;

/** Đếm Task còn sống của một Project (Q6 DATABASE_DESIGN §6). */
public record ProjectProgressCounts(long total, long completed) {

    /** Hàm dựng cho truy vấn JPQL "select new …": sum() trả null khi không có dòng. */
    public ProjectProgressCounts(Long total, Long completed) {
        this(total == null ? 0 : total, completed == null ? 0 : completed);
    }
}
