-- :name mymine-fetch-all-entries :?
-- :doc Fetch all of a user's mymine entries
select * from mymine
where user_id = :user-id
and mine = :mine

 -- :name mymine-add-entry :1
insert into mymine (user_id, mine, im_obj_type, im_obj_id, parent_id, label, open)
values (:user-id, :mine, :im-obj-type, :im-obj-id, :parent-id::uuid, :label, :open?)
returning *

 -- :name mymine-entry-update-open :<!
update mymine
set open = :open::boolean
where entry_id = :entry-id::uuid
returning *

 -- :name mymine-entry-update-label :<!
update mymine
set label = :label::string
where entry_id = :entry-id::uuid
returning *

 -- :name mymine-move-entry :<!
update mymine
set parent_id = :parent-id::uuid
where entry_id = :entry-id::uuid
returning *

 -- :name mymine-entry-delete-entry :<!
delete from mymine where entry_id = :entry-id::uuid
returning entry_id