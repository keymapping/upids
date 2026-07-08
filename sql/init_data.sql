INSERT INTO sys_user (username, password, real_name, role, status, created_at) 
VALUES ('admin', '$2b$12$gW3DD0En1UBnWLLjVe49Peae675qIaUktMyVtmyyZo4s2w5LdWCKm', '系统管理员', 'admin', 1, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

INSERT INTO sys_user (username, password, real_name, role, status, created_at) 
VALUES ('user', '$2b$12$gW3DD0En1UBnWLLjVe49Peae675qIaUktMyVtmyyZo4s2w5LdWCKm', '普通用户', 'user', 1, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;
