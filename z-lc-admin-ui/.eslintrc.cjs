/* eslint-env node */
module.exports = {
  root: true,
  env: { browser: true, es2021: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  plugins: ['react-refresh'],
  ignorePatterns: ['dist', 'node_modules', '*.cjs'],
  rules: {
    'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    '@typescript-eslint/no-explicit-any': 'error',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    '@typescript-eslint/consistent-type-imports': ['error', { prefer: 'type-imports' }],
    'no-console': ['error', { allow: ['warn', 'error'] }],
    eqeqeq: ['error', 'smart'],
  },
  overrides: [
    {
      // src/fields 是"字段类型注册表"：这些 .tsx 导出的是 FieldDefinition 数据
      // (渲染函数 + 算子 + config 子表单描述), 不是组件树里的组件。
      // react-refresh 的 only-export-components 针对的是页面组件文件, 用在这里
      // 会逼着把每个类型定义拆成两三个文件, 反而破坏注册表的内聚。
      files: ['src/fields/**/*.ts', 'src/fields/**/*.tsx'],
      rules: { 'react-refresh/only-export-components': 'off' },
    },
    {
      files: ['src/**/*.test.ts', 'src/**/*.test.tsx', 'src/test/**'],
      rules: { 'react-refresh/only-export-components': 'off' },
    },
  ],
};
