-- MIC Rhema: storage is accessed through the Firebase-authenticated Edge Function.
-- The service_role used by the function bypasses RLS; client roles remain denied.

DROP POLICY IF EXISTS "mic_rhema_profile_photos_private_gateway" ON storage.objects;
CREATE POLICY "mic_rhema_profile_photos_private_gateway"
ON storage.objects
FOR ALL
TO anon, authenticated
USING (bucket_id = 'profile-photos' AND false)
WITH CHECK (bucket_id = 'profile-photos' AND false);

DROP POLICY IF EXISTS "mic_rhema_church_documents_private_gateway" ON storage.objects;
CREATE POLICY "mic_rhema_church_documents_private_gateway"
ON storage.objects
FOR ALL
TO anon, authenticated
USING (bucket_id = 'church-documents' AND false)
WITH CHECK (bucket_id = 'church-documents' AND false);
