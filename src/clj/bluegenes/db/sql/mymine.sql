-- :name mymine-fetch-all-entries :?
-- :doc Fetch all of a user's mymine entries
select * from mymine
where user_id = :user-id
and mine = :mine

 -- :name mymine-add-entry
insert into mymine (user_id, mine, im_obj_type, im_obj_id, parent_id, label, open)
values (:user-id, :mine, :im-obj-type, :im-obj-id, :parent-id, :label, :open?)
returning entry_id