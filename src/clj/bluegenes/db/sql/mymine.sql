-- :name store-mymine-tree :insert
insert into mymine (user_id, mine, data) values (:user-id, :mine, :data)
on conflict (user_id, mine) do update set data = :data

-- :name fetch-mymine-tree :? :1
-- :doc Fetch a user's mymine tree by user id
select data from mymine
where user_id = :user-id
and mine = :mine