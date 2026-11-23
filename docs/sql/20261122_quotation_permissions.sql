-- 系统菜单和角色已初始化后执行。报价权限可从 ERP 角色授权页面分别分配。
BEGIN;

WITH quotation_menu (id, parent_id, hierarchy_suffix, type, name, i18n_code, path, component, permission_code, sort_order) AS (
    VALUES
        (2194011000000000000, 2088186609251737602, '2194011000000000000', 1, '报价管理',
            'menu.quotation', '/quotation-manager', 'quotation-manager', 'quotation-manager', 5),
        (2194011000000000001, 2194011000000000000, '2194011000000000000,2194011000000000001', 2, '报价单列表',
            'menu.quotation.list', '/quotation-manager/quotation', 'quotation', 'quotation-manager:quotation:query', 1),
        (2194011000000000002, 2194011000000000000, '2194011000000000000,2194011000000000002', 2, '个人模板',
            'menu.quotation.personal', '/quotation-manager/personal-template', 'personal-template', 'quotation-manager:personal-template:query', 2),
        (2194011000000000003, 2194011000000000000, '2194011000000000000,2194011000000000003', 2, '公共模板',
            'menu.quotation.public', '/quotation-manager/public-template', 'public-template', 'quotation-manager:public-template', 3),
        (2194011000000000004, 2194011000000000001, '2194011000000000000,2194011000000000001,2194011000000000004', 3, '新建报价',
            'menu.quotation.create', NULL, NULL, 'quotation-manager:quotation:create', 1),
        (2194011000000000005, 2194011000000000001, '2194011000000000000,2194011000000000001,2194011000000000005', 3, '修改报价',
            'menu.quotation.update', NULL, NULL, 'quotation-manager:quotation:update', 2),
        (2194011000000000006, 2194011000000000001, '2194011000000000000,2194011000000000001,2194011000000000006', 3, '删除报价',
            'menu.quotation.delete', NULL, NULL, 'quotation-manager:quotation:delete', 3),
        (2194011000000000007, 2194011000000000002, '2194011000000000000,2194011000000000002,2194011000000000007', 3, '创建个人模板',
            'menu.quotation.personal.create', NULL, NULL, 'quotation-manager:personal-template:create', 1),
        (2194011000000000008, 2194011000000000002, '2194011000000000000,2194011000000000002,2194011000000000008', 3, '修改个人模板',
            'menu.quotation.personal.update', NULL, NULL, 'quotation-manager:personal-template:update', 2),
        (2194011000000000009, 2194011000000000003, '2194011000000000000,2194011000000000003,2194011000000000009', 3, '创建公共模板',
            'menu.quotation.public.create', NULL, NULL, 'quotation-manager:public-template:create', 1),
        (2194011000000000010, 2194011000000000003, '2194011000000000000,2194011000000000003,2194011000000000010', 3, '修改公共模板',
            'menu.quotation.public.update', NULL, NULL, 'quotation-manager:public-template:update', 2)
)
INSERT INTO sys_menu (
    id, parent_id, hierarchy, client_id, type, name, i18n_code, path, component, permission_code, sort_order
)
SELECT
    menu.id, menu.parent_id, root.hierarchy || ',' || menu.hierarchy_suffix, root.client_id,
    menu.type, menu.name, menu.i18n_code, menu.path, menu.component, menu.permission_code, menu.sort_order
FROM quotation_menu menu
CROSS JOIN sys_menu root
WHERE root.id = 2088186609251737602 AND root.is_deleted = false
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_role_permission (id, role_id, menu_id, role_code, permission_code)
SELECT
    menu.id + 100, role.id, menu.id, role.role_code, menu.permission_code
FROM sys_menu menu
CROSS JOIN sys_role role
WHERE menu.id BETWEEN 2194011000000000000 AND 2194011000000000010
    AND menu.is_deleted = false AND role.role_code = 'SUPER_ADMIN' AND role.is_deleted = false
ON CONFLICT (role_id, menu_id) DO NOTHING;

COMMIT;
