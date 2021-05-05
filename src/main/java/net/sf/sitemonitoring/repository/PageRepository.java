package net.sf.sitemonitoring.repository;

import java.util.List;

import net.sf.sitemonitoring.entity.Page;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PageRepository extends JpaRepository<Page, Integer> {

	@Query("select p from Page p where p.id = ?1")
	Page findOne(int id);

	@Query("select distinct p from Page p left join fetch p.checks")
	List<Page> findAllFetchChecks();

}
