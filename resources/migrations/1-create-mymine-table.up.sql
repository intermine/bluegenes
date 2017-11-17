CREATE TABLE mymine (
  entry_id uuid UNIQUE DEFAULT uuid_generate_v4(),
  user_id integer NOT NULL,
  mine text NOT NULL,
  im_obj_type text,
  im_obj_id integer,
  parent_id uuid REFERENCES mymine(entry_id) ON DELETE CASCADE ON UPDATE CASCADE,
  label text,
  open boolean DEFAULT true,
  PRIMARY KEY (entry_id)
);

/*
 * Any combination of im_obj_type, im_obj_id, parent_id, and label
 * can be null, but postgres does not allow for primary keys that
 * include null values. So we create a custom index on those fields
 * that returns some string value if null is present. I think this rules
 * out the strings/integers/uuids below as valid column values.
 *
 * TODO: Am I going to hell for this?
 *
 */

CREATE UNIQUE INDEX mymine_entry_unique_constraint on mymine(
  user_id,
  mine,
  (coalesce(im_obj_type, '***NULL-IM_OBJ_TYPE***')),
  (coalesce(im_obj_id, 0)),
  (coalesce(parent_id, '00000000-0000-0000-0000-000000000000')),
  (coalesce(label, '***NULL-LABEL***'))
)